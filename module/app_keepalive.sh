#!/system/bin/sh

MODDIR="/data/adb/modules/android_optimization_module"
PKG_JAHEZ="net.jahez.fleets"
PKG_SANED="io.suqi8.saned"
PKG_MAPS="com.google.android.apps.maps"

. "$MODDIR/uid_helper.sh"

log_print() {
  echo "AppKeepAlive: $1" > /dev/kmsg
}

log_print "App KeepAlive Guard started."

# Initial whitelist
dumpsys deviceidle whitelist +$PKG_JAHEZ 2>/dev/null
dumpsys deviceidle whitelist +$PKG_SANED 2>/dev/null
dumpsys deviceidle whitelist +$PKG_MAPS 2>/dev/null

cmd appops set $PKG_JAHEZ RUN_IN_BACKGROUND allow 2>/dev/null
cmd appops set $PKG_SANED RUN_IN_BACKGROUND allow 2>/dev/null
cmd appops set $PKG_MAPS RUN_IN_BACKGROUND allow 2>/dev/null
cmd appops set $PKG_JAHEZ RUN_ANY_IN_BACKGROUND allow 2>/dev/null
cmd appops set $PKG_SANED RUN_ANY_IN_BACKGROUND allow 2>/dev/null

while true; do
    # Get UIDs — try cache file first, then detect
    UID_JAHEZ=$(cat /data/local/tmp/uid_jahez 2>/dev/null)
    UID_SANED=$(cat /data/local/tmp/uid_saned 2>/dev/null)

    [ -z "$UID_JAHEZ" ] && UID_JAHEZ=$(get_uid $PKG_JAHEZ)
    [ -z "$UID_SANED" ] && UID_SANED=$(get_uid $PKG_SANED)

    if [ -n "$UID_JAHEZ" ] && [ -n "$UID_SANED" ]; then
        # Save to cache
        echo "$UID_JAHEZ" > /data/local/tmp/uid_jahez
        echo "$UID_SANED" > /data/local/tmp/uid_saned

        # Remove data restrictions
        cmd netpolicy set uid-policy $UID_JAHEZ 0 2>/dev/null
        cmd netpolicy set uid-policy $UID_SANED 0 2>/dev/null
    fi

    # Process level protection
    for PKG in $PKG_JAHEZ $PKG_SANED; do
        for pid in $(pgrep -f "$PKG" 2>/dev/null); do
            echo -1000 > /proc/$pid/oom_score_adj 2>/dev/null
            renice -n -20 -p $pid 2>/dev/null
            taskset -p 0xC0 $pid 2>/dev/null
            ionice -c 1 -n 0 -p $pid 2>/dev/null
        done
        dumpsys deviceidle whitelist +$PKG 2>/dev/null
        cmd appops set $PKG RUN_IN_BACKGROUND allow 2>/dev/null
        cmd appops set $PKG RUN_ANY_IN_BACKGROUND allow 2>/dev/null
    done

    # Google Maps protection
    for pid in $(pgrep -f "$PKG_MAPS" 2>/dev/null); do
        echo -900 > /proc/$pid/oom_score_adj 2>/dev/null
        renice -n -10 -p $pid 2>/dev/null
    done

    sleep 5
done
