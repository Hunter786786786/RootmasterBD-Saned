#!/system/bin/sh

MODE=$1
[ -z "$MODE" ] && MODE="active"

PKG_JAHEZ="net.jahez.fleets"
PKG_SANED="io.suqi8.saned"

log_print() {
  echo "Android Optimization Module: $1" > /dev/kmsg
}

optimize_process() {
    local pkg=$1
    local mode=$2
    local pids=$(pidof $pkg)
    
    if [ -n "$pids" ]; then
        for pid in $pids; do
            if [ "$mode" = "active" ]; then
                renice -n -20 -p $pid
                ionice -c 1 -n 0 -p $pid
                taskset -p f0 $pid
                echo -1000 > /proc/$pid/oom_score_adj
            else
                # Revert to normal priority
                renice -n 0 -p $pid
                ionice -c 2 -n 4 -p $pid
                taskset -p ff $pid
                echo 0 > /proc/$pid/oom_score_adj
            fi
        done
    fi
}

if [ "$MODE" = "active" ]; then
    log_print "Applying process prioritization..."
    # Whitelist from Doze mode
    dumpsys deviceidle whitelist +$PKG_JAHEZ
    dumpsys deviceidle whitelist +$PKG_SANED
else
    log_print "Removing process prioritization..."
    dumpsys deviceidle whitelist -$PKG_JAHEZ
    dumpsys deviceidle whitelist -$PKG_SANED
fi

# Run once if called manually, otherwise loop if it's the main service call
# We check if we are in the background (service.sh call)
while true; do
    # Read current state from file to handle dynamic changes
    CURRENT_MODE=$(cat "/data/adb/modules/android_optimization_module/perf_state" 2>/dev/null || echo "idle")
    optimize_process $PKG_JAHEZ $CURRENT_MODE
    optimize_process $PKG_SANED $CURRENT_MODE
    sleep 30
done
