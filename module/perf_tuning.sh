#!/system/bin/sh

# =====================================================
# RootmasterBD Performance Tuner
# Device-aware CPU/GPU locking
# =====================================================

MODDIR="/data/adb/modules/android_optimization_module"
DEVICE_FILE="/data/local/tmp/rm_device_profile"
STATE=${1:-"idle"}

log_print() {
    echo "PerfTuner: $1" > /dev/kmsg
}

# Load device profile
load_profile() {
    if [ ! -f "$DEVICE_FILE" ]; then
        sh "$MODDIR/device_detect.sh" > /dev/null 2>&1
    fi
    [ -f "$DEVICE_FILE" ] && . "$DEVICE_FILE"
}

apply_active() {
    load_profile

    log_print "Applying ACTIVE mode for: ${DEVICE_TAG:-Unknown}"

    # Device-specific frequency overrides
    case "$DEVICE_TAG" in
        Xiaomi15Ultra|Xiaomi15Pro|Xiaomi15)
            # Xiaomi 15 Ultra: Prime 4320MHz, Perf 3530MHz, GPU Adreno 830
            [ -z "$CPU_PRIME_MAX" ] && CPU_PRIME_MAX=4320000
            [ -z "$CPU_PERF_MAX" ] && CPU_PERF_MAX=3530000
            [ -z "$GPU_MAX" ] && GPU_MAX=1100000000
            ;;
        RedMagic11Pro)
            # Red Magic 11 Pro: Prime 4608MHz (OC version)
            [ -z "$CPU_PRIME_MAX" ] && CPU_PRIME_MAX=4608000
            [ -z "$CPU_PERF_MAX" ] && CPU_PERF_MAX=3187200
            [ -z "$GPU_MAX" ] && GPU_MAX=1100000000
            ;;
        OnePlus15)
            # OnePlus 15: Prime 4320MHz
            [ -z "$CPU_PRIME_MAX" ] && CPU_PRIME_MAX=4320000
            [ -z "$CPU_PERF_MAX" ] && CPU_PERF_MAX=3530000
            [ -z "$GPU_MAX" ] && GPU_MAX=1100000000
            ;;
        GalaxyS25Ultra|GalaxyS25)
            # Galaxy S25: Snapdragon 8 Elite 4320MHz
            [ -z "$CPU_PRIME_MAX" ] && CPU_PRIME_MAX=4320000
            [ -z "$CPU_PERF_MAX" ] && CPU_PERF_MAX=3530000
            [ -z "$GPU_MAX" ] && GPU_MAX=1100000000
            ;;
        *)
            # Generic — use detected values
            ;;
    esac

    # ── CPU Prime cores lock ──────────────────────────
    if [ -n "$CPU_PRIME_POLICY" ] && [ -d "/sys/devices/system/cpu/cpufreq/policy${CPU_PRIME_POLICY}" ]; then
        local pdir="/sys/devices/system/cpu/cpufreq/policy${CPU_PRIME_POLICY}"
        echo "performance" > $pdir/scaling_governor 2>/dev/null
        echo "$CPU_PRIME_MAX" > $pdir/scaling_min_freq 2>/dev/null
        echo "$CPU_PRIME_MAX" > $pdir/scaling_max_freq 2>/dev/null
        log_print "Prime cores locked: policy${CPU_PRIME_POLICY} @ ${CPU_PRIME_MAX}KHz"
    fi

    # ── CPU Performance cores lock ────────────────────
    if [ -n "$CPU_PERF_POLICY" ] && [ -d "/sys/devices/system/cpu/cpufreq/policy${CPU_PERF_POLICY}" ]; then
        local pdir="/sys/devices/system/cpu/cpufreq/policy${CPU_PERF_POLICY}"
        echo "performance" > $pdir/scaling_governor 2>/dev/null
        echo "$CPU_PERF_MAX" > $pdir/scaling_min_freq 2>/dev/null
        echo "$CPU_PERF_MAX" > $pdir/scaling_max_freq 2>/dev/null
        log_print "Perf cores locked: policy${CPU_PERF_POLICY} @ ${CPU_PERF_MAX}KHz"
    fi

    # ── CPU Efficiency cores lock ─────────────────────
    if [ -n "$CPU_EFF_POLICY" ] && [ -d "/sys/devices/system/cpu/cpufreq/policy${CPU_EFF_POLICY}" ]; then
        local pdir="/sys/devices/system/cpu/cpufreq/policy${CPU_EFF_POLICY}"
        echo "performance" > $pdir/scaling_governor 2>/dev/null
        echo "$CPU_EFF_MAX" > $pdir/scaling_min_freq 2>/dev/null
        echo "$CPU_EFF_MAX" > $pdir/scaling_max_freq 2>/dev/null
    fi

    # ── Boost ─────────────────────────────────────────
    [ -n "$BOOST_PATH" ] && [ -f "$BOOST_PATH" ] && echo "1" > "$BOOST_PATH" 2>/dev/null

    # ── GPU lock ──────────────────────────────────────
    if [ -n "$GPU_PATH" ] && [ -n "$GPU_MAX" ]; then
        if [ -d "/sys/class/devfreq/$GPU_PATH" ]; then
            echo "performance" > /sys/class/devfreq/$GPU_PATH/governor 2>/dev/null
            echo "$GPU_MAX" > /sys/class/devfreq/$GPU_PATH/min_freq 2>/dev/null
            echo "$GPU_MAX" > /sys/class/devfreq/$GPU_PATH/max_freq 2>/dev/null
            log_print "GPU locked: $GPU_PATH @ ${GPU_MAX}Hz"
        elif [ -d "/sys/class/kgsl/kgsl-3d0" ]; then
            echo "performance" > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null
            echo "$GPU_MAX" > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq 2>/dev/null
        fi
    fi

    # ── Thermal — raise threshold ─────────────────────
    local tpath="${THERMAL_PATH:-/sys/class/thermal}"
    for tz in $tpath/thermal_zone*; do
        [ -f "$tz/trip_point_0_temp" ] && echo 95000 > "$tz/trip_point_0_temp" 2>/dev/null
        [ -f "$tz/trip_point_1_temp" ] && echo 100000 > "$tz/trip_point_1_temp" 2>/dev/null
        [ -f "$tz/trip_point_2_temp" ] && echo 105000 > "$tz/trip_point_2_temp" 2>/dev/null
    done

    # ── 144Hz ─────────────────────────────────────────
    setprop persist.sys.display.refresh_rate 144 2>/dev/null
    setprop persist.vendor.display.refresh_rate 144 2>/dev/null
    service call SurfaceFlinger 1035 i32 144 2>/dev/null

    # ── Memory ────────────────────────────────────────
    echo "0" > /proc/sys/vm/swappiness 2>/dev/null
    setprop persist.sys.power.mode 1 2>/dev/null
    setprop persist.sys.extreme.mode 1 2>/dev/null

    log_print "ACTIVE mode applied for $DEVICE_TAG"
}

apply_idle() {
    load_profile

    # ── Restore governors ─────────────────────────────
    for p in /sys/devices/system/cpu/cpufreq/policy*; do
        [ -d "$p" ] || continue
        echo "schedutil" > $p/scaling_governor 2>/dev/null
        local pmin=$(cat $p/cpuinfo_min_freq 2>/dev/null || echo 300000)
        local pmax=$(cat $p/cpuinfo_max_freq 2>/dev/null || echo 3000000)
        echo "$pmin" > $p/scaling_min_freq 2>/dev/null
        echo "$pmax" > $p/scaling_max_freq 2>/dev/null
    done

    # ── Boost off ─────────────────────────────────────
    [ -n "$BOOST_PATH" ] && [ -f "$BOOST_PATH" ] && echo "0" > "$BOOST_PATH" 2>/dev/null

    # ── GPU restore ───────────────────────────────────
    if [ -n "$GPU_PATH" ] && [ -d "/sys/class/devfreq/$GPU_PATH" ]; then
        echo "msm-adreno-tz" > /sys/class/devfreq/$GPU_PATH/governor 2>/dev/null
        local gmin=$(cat /sys/class/devfreq/$GPU_PATH/min_freq 2>/dev/null || echo 150000000)
        echo "$gmin" > /sys/class/devfreq/$GPU_PATH/min_freq 2>/dev/null
    fi

    # ── Memory restore ────────────────────────────────
    echo "60" > /proc/sys/vm/swappiness 2>/dev/null
    setprop persist.sys.power.mode 0 2>/dev/null

    log_print "IDLE mode applied"
}

case "$STATE" in
    active) apply_active ;;
    idle)   apply_idle ;;
    detect) sh "$MODDIR/device_detect.sh" ;;
    *)      apply_idle ;;
esac
