#!/system/bin/sh

MODDIR=${0%/*}

# Wait for /data to be mounted
until [ -d /data/vendor ]; do
  sleep 1
done

# Robust SoC Detection
SOC_MODEL=$(getprop ro.soc.model)
[ -z "$SOC_MODEL" ] && SOC_MODEL=$(getprop ro.vendor.qcom.platform)
[ -z "$SOC_MODEL" ] && SOC_MODEL=$(getprop ro.board.platform)
[ -z "$SOC_MODEL" ] && SOC_MODEL=$(getprop ro.mediatek.platform)

log_print "Detected SoC: $SOC_MODEL"

# Apply performance profile for all detected SoCs (Universal approach)
if [ -n "$SOC_MODEL" ]; then
    log_print "Snapdragon SoC detected. Applying performance profile..."
    # Apply performance tuning based on saved state
    STATE=$(cat "$MODDIR/perf_state" 2>/dev/null || echo "idle")
    if [ "$STATE" = "active" ]; then
        sh $MODDIR/perf_tuning.sh "active"
    else
        sh $MODDIR/perf_tuning.sh "idle"
    fi
fi

# Ensure all scripts are executable
chmod 755 $MODDIR/*.sh

log_print() {
  echo "Android Optimization Module: $1" > /dev/kmsg
}
