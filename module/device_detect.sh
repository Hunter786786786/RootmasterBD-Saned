#!/system/bin/sh

# =====================================================
# RootmasterBD Device Detector
# Auto detects device and applies correct CPU/GPU paths
# =====================================================

DEVICE_FILE="/data/local/tmp/rm_device_profile"

detect_device() {
    local model=$(getprop ro.product.model 2>/dev/null)
    local board=$(getprop ro.product.board 2>/dev/null)
    local soc=$(getprop ro.board.platform 2>/dev/null)
    local device=$(getprop ro.product.device 2>/dev/null)
    local chipname=$(getprop ro.soc.model 2>/dev/null)

    echo "DEVICE_MODEL=$model"
    echo "DEVICE_BOARD=$board"
    echo "DEVICE_SOC=$soc"
    echo "DEVICE_NAME=$device"
    echo "CHIP_NAME=$chipname"

    # ── Detect CPU policy structure ──────────────────
    local prime_policy=""
    local perf_policy=""
    local eff_policy=""

    # Find policies
    for p in /sys/devices/system/cpu/cpufreq/policy*; do
        [ -d "$p" ] || continue
        local pmax=$(cat $p/cpuinfo_max_freq 2>/dev/null || echo 0)
        local pnum=$(echo $p | grep -oE '[0-9]+$')

        if [ "$pmax" -gt 4000000 ]; then
            prime_policy=$pnum
            echo "CPU_PRIME_POLICY=$pnum"
            echo "CPU_PRIME_MAX=$pmax"
        elif [ "$pmax" -gt 2500000 ]; then
            perf_policy=$pnum
            echo "CPU_PERF_POLICY=$pnum"
            echo "CPU_PERF_MAX=$pmax"
        else
            eff_policy=$pnum
            echo "CPU_EFF_POLICY=$pnum"
            echo "CPU_EFF_MAX=$pmax"
        fi
    done

    # ── Detect GPU path ──────────────────────────────
    local gpu_path=""
    if [ -d "/sys/class/devfreq/3d00000.qcom,kgsl-3d0" ]; then
        gpu_path="3d00000.qcom,kgsl-3d0"
    elif [ -d "/sys/class/devfreq/5000000.qcom,kgsl-3d0" ]; then
        gpu_path="5000000.qcom,kgsl-3d0"
    elif [ -d "/sys/class/kgsl/kgsl-3d0" ]; then
        gpu_path="kgsl-3d0"
    fi
    echo "GPU_PATH=$gpu_path"

    # ── Detect GPU max freq ──────────────────────────
    local gpu_max=""
    if [ -n "$gpu_path" ] && [ -f "/sys/class/devfreq/$gpu_path/max_freq" ]; then
        gpu_max=$(cat /sys/class/devfreq/$gpu_path/max_freq 2>/dev/null)
    elif [ -f "/sys/class/kgsl/kgsl-3d0/max_gpuclk" ]; then
        gpu_max=$(cat /sys/class/kgsl/kgsl-3d0/max_gpuclk 2>/dev/null)
    fi
    echo "GPU_MAX=$gpu_max"

    # ── Detect thermal path ──────────────────────────
    if [ -d "/sys/devices/virtual/thermal" ]; then
        echo "THERMAL_PATH=/sys/devices/virtual/thermal"
    else
        echo "THERMAL_PATH=/sys/class/thermal"
    fi

    # ── Detect boost interface ───────────────────────
    if [ -f "/sys/devices/system/cpu/cpufreq/boost" ]; then
        echo "BOOST_PATH=/sys/devices/system/cpu/cpufreq/boost"
    else
        echo "BOOST_PATH="
    fi

    # ── Device name tag ──────────────────────────────
    case "$model" in
        *"Red Magic 11 Pro"*|*NX809J*) echo "DEVICE_TAG=RedMagic11Pro" ;;
        *"Red Magic 10 Pro"*|*NX769J*) echo "DEVICE_TAG=RedMagic10Pro" ;;
        *"Red Magic 9 Pro"*) echo "DEVICE_TAG=RedMagic9Pro" ;;
        *"OnePlus 15"*) echo "DEVICE_TAG=OnePlus15" ;;
        *"OnePlus 13"*) echo "DEVICE_TAG=OnePlus13" ;;
        *"15 Ultra"*|*23116PN5BC*|*xuanyuan*) echo "DEVICE_TAG=Xiaomi15Ultra" ;;
        *"Xiaomi 15 Pro"*) echo "DEVICE_TAG=Xiaomi15Pro" ;;
        *"Xiaomi 15"*) echo "DEVICE_TAG=Xiaomi15" ;;
        *"14 Ultra"*) echo "DEVICE_TAG=Xiaomi14Ultra" ;;
        *"Xiaomi 14"*) echo "DEVICE_TAG=Xiaomi14" ;;
        *"Galaxy S25 Ultra"*) echo "DEVICE_TAG=GalaxyS25Ultra" ;;
        *"Galaxy S25"*) echo "DEVICE_TAG=GalaxyS25" ;;
        *"POCO F6 Pro"*) echo "DEVICE_TAG=POCOf6Pro" ;;
        *) echo "DEVICE_TAG=Generic_$soc" ;;
    esac
}

# Generate device profile
detect_device > "$DEVICE_FILE"
chmod 644 "$DEVICE_FILE"

echo "Device profile saved to $DEVICE_FILE"
cat "$DEVICE_FILE"
