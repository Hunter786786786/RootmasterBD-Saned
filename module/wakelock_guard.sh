#!/system/bin/sh

MODDIR="/data/adb/modules/android_optimization_module"
PKG_JAHEZ="net.jahez.fleets"
PKG_SANED="io.suqi8.saned"

. "$MODDIR/uid_helper.sh"

log_print() {
  echo "WakelockGuard: $1" > /dev/kmsg
}

log_print "Wakelock Guard started."

while true; do
    JAHEZ_RUNNING=$(pgrep -f "$PKG_JAHEZ" 2>/dev/null)
    SANED_RUNNING=$(pgrep -f "$PKG_SANED" 2>/dev/null)

    if [ -n "$JAHEZ_RUNNING" ] || [ -n "$SANED_RUNNING" ]; then
        # Keep CPU awake
        echo "AndroidOptModule" > /sys/power/wake_lock 2>/dev/null
        # Mobile data always connected
        setprop net.data.default.dormancy 0 2>/dev/null
        echo "0" > /sys/class/net/rmnet_data0/dormancy_timeout 2>/dev/null
        # Re-apply network permissions every loop
        UID_J=$(cat /data/local/tmp/uid_jahez 2>/dev/null)
        UID_S=$(cat /data/local/tmp/uid_saned 2>/dev/null)
        [ -z "$UID_J" ] && UID_J=$(get_uid $PKG_JAHEZ)
        [ -z "$UID_S" ] && UID_S=$(get_uid $PKG_SANED)
        if [ -n "$UID_J" ] && [ -n "$UID_S" ]; then
            cmd netpolicy set uid-policy $UID_J 0 2>/dev/null
            cmd netpolicy set uid-policy $UID_S 0 2>/dev/null
        fi
        # Prevent doze
        dumpsys deviceidle whitelist +$PKG_JAHEZ 2>/dev/null
        dumpsys deviceidle whitelist +$PKG_SANED 2>/dev/null
    else
        echo "AndroidOptModule" > /sys/power/wake_unlock 2>/dev/null
    fi
    sleep 2
done
