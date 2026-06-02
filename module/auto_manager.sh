#!/system/bin/sh

MODDIR="/data/adb/modules/android_optimization_module"
PKG_JAHEZ="net.jahez.fleets"
PKG_SANED="io.suqi8.saned"
LAST_STATE="none"
STATE_FILE="$MODDIR/perf_state"

log_print() {
  echo "Android Optimization Module [Auto]: $1" > /dev/kmsg
}

is_running() {
    local pkg=$1
    # Method 1: pgrep
    pgrep -f "$pkg" > /dev/null 2>&1 && return 0
    # Method 2: ps -A
    ps -A 2>/dev/null | grep -q "$pkg" && return 0
    # Method 3: pidof
    pidof "$pkg" > /dev/null 2>&1 && return 0
    # Method 4: proc cmdline scan
    for f in /proc/*/cmdline; do
        local cmd=$(cat "$f" 2>/dev/null | tr -d '"'"'\0'"'"')
        echo "$cmd" | grep -q "^${pkg}" && return 0
    done
    # Method 5: dumpsys
    dumpsys activity processes 2>/dev/null | grep -q "$pkg" && return 0
    return 1
}

while true; do
    if is_running "$PKG_JAHEZ" || is_running "$PKG_SANED"; then
        CURRENT_NEED="active"
    else
        CURRENT_NEED="idle"
    fi

    if [ "$CURRENT_NEED" != "$LAST_STATE" ]; then
        if [ "$CURRENT_NEED" = "active" ]; then
            log_print "Target apps detected. Activating Performance Mode."
            echo "active" > "$STATE_FILE"
            sh "$MODDIR/performance_toggle.sh" "active" 2>/dev/null
        else
            log_print "Target apps closed. Deactivating Performance Mode."
            echo "idle" > "$STATE_FILE"
            sh "$MODDIR/performance_toggle.sh" "idle" 2>/dev/null
        fi
        LAST_STATE="$CURRENT_NEED"
        log_print "State changed to: $CURRENT_NEED"
    fi

    sleep 1
done
