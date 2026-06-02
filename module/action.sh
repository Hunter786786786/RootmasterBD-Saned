#!/system/bin/sh

MODDIR="/data/adb/modules/android_optimization_module"
STATE_FILE="$MODDIR/perf_state"

CURRENT=$(cat "$STATE_FILE" 2>/dev/null || echo "idle")

if [ "$CURRENT" = "active" ]; then
    echo "idle" > "$STATE_FILE"
    sh "$MODDIR/performance_toggle.sh" idle 2>/dev/null
    echo "BALANCED MODE — Performance OFF"
else
    echo "active" > "$STATE_FILE"
    sh "$MODDIR/performance_toggle.sh" active 2>/dev/null
    echo "ACTIVE MODE — Performance ON"
fi
