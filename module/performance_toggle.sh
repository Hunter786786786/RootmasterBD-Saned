#!/system/bin/sh

# Dynamically detect module directory
if [ -d "/data/adb/modules/android_optimization_module" ]; then
    MODDIR="/data/adb/modules/android_optimization_module"
elif [ -d "/data/adb/modules_update/android_optimization_module" ]; then
    MODDIR="/data/adb/modules_update/android_optimization_module"
else
    MODDIR=$(dirname $(readlink -f "$0"))
fi
STATE_FILE="/data/adb/modules/android_optimization_module/perf_state"

# Initialize state if it doesn't exist
if [ ! -f "$STATE_FILE" ]; then
    echo "idle" > "$STATE_FILE"
fi

CURRENT_STATE=$(cat "$STATE_FILE")

apply_active() {
    # Only apply if not already active to avoid redundant logs/overhead
    if [ "$CURRENT_STATE" != "active" ]; then
        echo "active" > "$STATE_FILE"
        sh "$MODDIR/perf_tuning.sh" "active"
        sh "$MODDIR/process_priority.sh" "active"
    fi
    echo "[Performance Mode: ACTIVATED]"
}

apply_idle() {
    # Only apply if not already idle
    if [ "$CURRENT_STATE" != "idle" ]; then
        echo "idle" > "$STATE_FILE"
        sh "$MODDIR/perf_tuning.sh" "idle"
        sh "$MODDIR/process_priority.sh" "idle"
    fi
    echo "[Performance Mode: DEACTIVATED]"
}

if [ "$1" = "active" ]; then
    apply_active
elif [ "$1" = "idle" ]; then
    apply_idle
else
    # Toggle logic
    if [ "$CURRENT_STATE" = "active" ]; then
        apply_idle
    else
        apply_active
    fi
fi
