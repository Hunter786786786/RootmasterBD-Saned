#!/system/bin/sh

MODDIR="/data/adb/modules/android_optimization_module"
DATA_FILE="/data/local/tmp/android_perf_data"
PKG_JAHEZ="net.jahez.fleets"
PKG_SANED="io.suqi8.saned"
PKG_MAPS="com.google.android.apps.maps"

. "$MODDIR/uid_helper.sh"

CACHED_UID_J=""
CACHED_UID_S=""

get_uids() {
    CACHED_UID_J=$(cat /data/local/tmp/uid_jahez 2>/dev/null)
    CACHED_UID_S=$(cat /data/local/tmp/uid_saned 2>/dev/null)
    [ -z "$CACHED_UID_J" ] && CACHED_UID_J=$(get_uid $PKG_JAHEZ)
    [ -z "$CACHED_UID_S" ] && CACHED_UID_S=$(get_uid $PKG_SANED)
    [ -n "$CACHED_UID_J" ] && echo "$CACHED_UID_J" > /data/local/tmp/uid_jahez && chmod 644 /data/local/tmp/uid_jahez
    [ -n "$CACHED_UID_S" ] && echo "$CACHED_UID_S" > /data/local/tmp/uid_saned && chmod 644 /data/local/tmp/uid_saned
}

get_active_iface() {
    local iface=$(ip route get 1.1.1.1 2>/dev/null | grep dev | awk '{print $5}')
    [ -n "$iface" ] && echo "$iface" && return
    for c in rmnet_data1 rmnet_data2 rmnet_data0 rmnet_data3; do
        ip link show $c 2>/dev/null | grep -q "UP" && echo "$c" && return
    done
}

get_process_status() {
    local pkg=$1
    # Foreground check
    if dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp|mResumedActivity' | grep -q "$pkg"; then
        echo "Foreground"
        return
    fi
    # Multiple running checks
    if pgrep -f "$pkg" > /dev/null 2>&1; then
        echo "Background"
        return
    fi
    if ps -A 2>/dev/null | grep -q "$pkg"; then
        echo "Background"
        return
    fi
    # proc cmdline scan
    for f in /proc/*/cmdline; do
        local cmd=$(cat "$f" 2>/dev/null | tr -d '"'"'\0'"'"')
        if echo "$cmd" | grep -q "^${pkg}"; then
            echo "Background"
            return
        fi
    done
    echo "Inactive"
}

get_net_speed() {
    local dev=$(get_active_iface)
    [ -z "$dev" ] && echo "0|0" && return
    local r1=$(cat /sys/class/net/$dev/statistics/rx_bytes 2>/dev/null || echo 0)
    local t1=$(cat /sys/class/net/$dev/statistics/tx_bytes 2>/dev/null || echo 0)
    sleep 1
    local r2=$(cat /sys/class/net/$dev/statistics/rx_bytes 2>/dev/null || echo 0)
    local t2=$(cat /sys/class/net/$dev/statistics/tx_bytes 2>/dev/null || echo 0)
    echo "$(( (r2-r1)/1024 ))|$(( (t2-t1)/1024 ))"
}

get_ms() {
    # General latency (Cloudflare)
    local results=$(ping -c 3 -i 0.2 -w 3 1.1.1.1 2>/dev/null | grep 'time=' | awk -F'time=' '{print $2}' | cut -d. -f1)
    [ -z "$results" ] && echo "N/A|N/A" && return
    local general=$(echo "$results" | sort -n | head -1)
    # Jahez server latency
    local jahez_host="api.jahez.net"
    local jahez_results=$(ping -c 3 -i 0.2 -w 3 $jahez_host 2>/dev/null | grep 'time=' | awk -F'time=' '{print $2}' | cut -d. -f1)
    local jahez_ms="N/A"
    [ -n "$jahez_results" ] && jahez_ms=$(echo "$jahez_results" | sort -n | head -1)
    echo "${general}ms|${jahez_ms}ms"
}

get_hz() {
    local hz=$(dumpsys display 2>/dev/null | grep -E "mRefreshRate|refreshRate" | grep -oE "[0-9]+\.[0-9]+" | head -1 | cut -d. -f1)
    [ -n "$hz" ] && [ "$hz" -gt 0 ] && [ "$hz" -lt 500 ] && echo "$hz" && return
    echo "144"
}

get_fps() {
    local fps=$(dumpsys SurfaceFlinger 2>/dev/null | grep -E "fps" | grep -oE "[0-9]+\.[0-9]+" | awk '{if($1>0 && $1<200) print int($1)}' | head -1)
    [ -n "$fps" ] && echo "$fps" || echo "N/A"
}

get_oryon() {
    # Use device profile to get prime policy
    local prime_policy=$(grep "CPU_PRIME_POLICY" /data/local/tmp/rm_device_profile 2>/dev/null | cut -d= -f2)
    if [ -n "$prime_policy" ] && [ -f "/sys/devices/system/cpu/cpufreq/policy${prime_policy}/scaling_cur_freq" ]; then
        local freq=$(cat /sys/devices/system/cpu/cpufreq/policy${prime_policy}/scaling_cur_freq 2>/dev/null || echo 0)
        echo "$((freq/1000))|$((freq/1000))"
    else
        local f6=$(cat /sys/devices/system/cpu/cpu6/cpufreq/scaling_cur_freq 2>/dev/null || echo "0")
        local f7=$(cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq 2>/dev/null || echo "0")
        echo "$((f6/1000))|$((f7/1000))"
    fi
}

check_wakelock() {
    cat /sys/power/wake_lock 2>/dev/null | grep -q "AndroidOptModule" && echo "ON" || echo "OFF"
}

check_gps() {
    local mode=$(settings get secure location_mode 2>/dev/null)
    [ "$mode" = "3" ] && echo "HIGH_ACC" && return
    [ "$mode" = "1" ] && echo "GPS_ONLY" && return
    echo "OFF"
}

check_keepalive() {
    [ -z "$CACHED_UID_J" ] && echo "NO_UID" && return
    # Check Jahez first, then Saned
    local pid=$(pgrep -f $PKG_JAHEZ 2>/dev/null | head -1)
    [ -z "$pid" ] && pid=$(pgrep -f $PKG_SANED 2>/dev/null | head -1)
    [ -z "$pid" ] && echo "BOTH_OFF" && return
    local oom=$(cat /proc/$pid/oom_score_adj 2>/dev/null)
    [ "$oom" = "-1000" ] && echo "PROTECTED" || echo "OOM:$oom"
}

check_bg_data() {
    [ -z "$CACHED_UID_J" ] && echo "NO_UID" && return
    local result=$(cmd appops get $PKG_JAHEZ RUN_ANY_IN_BACKGROUND 2>/dev/null)
    echo "$result" | grep -qi "allow" && echo "ALLOWED" || echo "RESTRICTED"
}

COUNTER=0
while true; do
    M=$(cat "$MODDIR/perf_state" 2>/dev/null || echo "idle")
    # Check kernel bridge — if kernel fusion active, skip redundant tweaks
    KERNEL_FUSION=$(grep "kernel_fusion" /data/local/tmp/kernel_bridge 2>/dev/null | cut -d= -f2)
    COUNTER=$((COUNTER + 1))

    [ "$((COUNTER % 30))" -eq 0 ] || [ -z "$CACHED_UID_J" ] && get_uids

    if [ "$M" = "active" ]; then
        # CPU/GPU lock via device-aware perf_tuner
        sh "$MODDIR/perf_tuning.sh" active 2>/dev/null

        # GPU lock — Red Magic 11 Pro
        # via kgsl
        if [ -d "/sys/class/kgsl/kgsl-3d0" ]; then
            GPU_MAX=$(cat /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null || echo "1100000000")
            echo "performance" > /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null
            echo "$GPU_MAX" > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq 2>/dev/null
        fi
        # via devfreq
        if [ -d "/sys/class/devfreq/3d00000.qcom,kgsl-3d0" ]; then
            GPU_MAX=$(cat /sys/class/devfreq/3d00000.qcom,kgsl-3d0/max_freq 2>/dev/null || echo "1100000000")
            echo "performance" > /sys/class/devfreq/3d00000.qcom,kgsl-3d0/governor 2>/dev/null
            echo "$GPU_MAX" > /sys/class/devfreq/3d00000.qcom,kgsl-3d0/min_freq 2>/dev/null
        fi

        # Thermal off
        # Thermal — raise threshold to 95°C, do NOT disable cooling devices
        # This allows CPU to run at max until 95°C, then safely throttle
        for tz in /sys/devices/virtual/thermal/thermal_zone*; do
            # Raise trip point 0 to 95°C
            [ -f "$tz/trip_point_0_temp" ] && echo 95000 > "$tz/trip_point_0_temp" 2>/dev/null
            # Raise trip point 1 to 100°C
            [ -f "$tz/trip_point_1_temp" ] && echo 100000 > "$tz/trip_point_1_temp" 2>/dev/null
            # Raise trip point 2 to 105°C
            [ -f "$tz/trip_point_2_temp" ] && echo 105000 > "$tz/trip_point_2_temp" 2>/dev/null
        done
        # Also check /sys/class/thermal
        for tz in /sys/class/thermal/thermal_zone*; do
            [ -f "$tz/trip_point_0_temp" ] && echo 95000 > "$tz/trip_point_0_temp" 2>/dev/null
            [ -f "$tz/trip_point_1_temp" ] && echo 100000 > "$tz/trip_point_1_temp" 2>/dev/null
        done

        # Process priority
        for pid in $(pgrep -f $PKG_JAHEZ 2>/dev/null); do
            renice -n -20 -p $pid 2>/dev/null
            taskset -p 0xC0 $pid 2>/dev/null
            ionice -c 1 -n 0 -p $pid 2>/dev/null
            echo -1000 > /proc/$pid/oom_score_adj 2>/dev/null
        done
        for pid in $(pgrep -f $PKG_SANED 2>/dev/null); do
            renice -n -20 -p $pid 2>/dev/null
            taskset -p 0xC0 $pid 2>/dev/null
            ionice -c 1 -n 0 -p $pid 2>/dev/null
            echo -1000 > /proc/$pid/oom_score_adj 2>/dev/null
        done

        # 144Hz
        setprop persist.sys.display.refresh_rate 144 2>/dev/null
        setprop persist.vendor.display.refresh_rate 144 2>/dev/null
        service call SurfaceFlinger 1035 i32 144 2>/dev/null
        am set-refresh-rate $PKG_JAHEZ 144 2>/dev/null
        am set-refresh-rate $PKG_SANED 144 2>/dev/null
        settings put system peak_refresh_rate_for_package $PKG_JAHEZ 144 2>/dev/null
        settings put system min_refresh_rate_for_package $PKG_JAHEZ 144 2>/dev/null
        settings put system peak_refresh_rate_for_package $PKG_SANED 144 2>/dev/null
        settings put system min_refresh_rate_for_package $PKG_SANED 144 2>/dev/null

        # Force 144fps for Jahez and Saned specifically
        # Other apps use system default (60/90fps)
        dumpsys SurfaceFlinger --set-frame-rate $PKG_JAHEZ 144 2>/dev/null
        dumpsys SurfaceFlinger --set-frame-rate $PKG_SANED 144 2>/dev/null
        # Reduce frame rate for background apps to save resources for Jahez
        for pid in $(pgrep -v -f "$PKG_JAHEZ|$PKG_SANED|$PKG_MAPS|system|surfaceflinger" 2>/dev/null | head -20); do
            app_pkg=$(cat /proc/$pid/cmdline 2>/dev/null | tr -d ' ' | cut -d: -f1)
            [ -n "$app_pkg" ] && settings put system peak_refresh_rate_for_package $app_pkg 60 2>/dev/null
        done

        echo "0" > /proc/sys/vm/swappiness 2>/dev/null
        setprop persist.sys.power.mode 1 2>/dev/null
        setprop persist.sys.extreme.mode 1 2>/dev/null

        # Re-apply background data allow every loop
        if [ -n "$CACHED_UID_J" ]; then
            cmd appops set $PKG_JAHEZ RUN_IN_BACKGROUND allow 2>/dev/null
            cmd appops set $PKG_SANED RUN_IN_BACKGROUND allow 2>/dev/null
            cmd appops set $PKG_JAHEZ RUN_ANY_IN_BACKGROUND allow 2>/dev/null
            cmd appops set $PKG_SANED RUN_ANY_IN_BACKGROUND allow 2>/dev/null
        fi
    fi

    # Collect data
    J=$(get_process_status $PKG_JAHEZ)
    S=$(get_process_status $PKG_SANED)
    MAPS_S=$(get_process_status $PKG_MAPS)
    C=$(uptime | awk -F'load average:' '{print $2}' | cut -d, -f1 | sed 's/ //g')
    G=$(cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage 2>/dev/null | cut -d' ' -f1 || echo "0")
    O=$(get_oryon)
    N=$(get_net_speed)
    MS=$(get_ms)
    HZ=$(get_hz)
    FPS=$(get_fps)
    WL=$(check_wakelock)
    GPS=$(check_gps)
    KA=$(check_keepalive)
    BG=$(check_bg_data)
    IFACE=$(get_active_iface)
    UID_DISPLAY="${CACHED_UID_J:-N/A}|${CACHED_UID_S:-N/A}"

    {
        echo "MODE:$M"
        echo "CPU:$C"
        echo "GPU:$G"
        echo "ORYON:$O"
        echo "JAHEZ:$J"
        echo "SANED:$S"
        echo "MAPS:$MAPS_S"
        echo "NET:$N"
        echo "MS_NET:$(echo $MS | cut -d'|' -f1)"
        echo "MS_JAHEZ:$(echo $MS | cut -d'|' -f2)"
        echo "HZ:$HZ"
        echo "FPS:$FPS"
        echo "WAKELOCK:$WL"
        echo "GPS:$GPS"
        echo "KEEPALIVE:$KA"
        echo "BGDATA:$BG"
        echo "UID:$UID_DISPLAY"
        echo "IFACE:$IFACE"
    } > "$DATA_FILE.tmp"

    mv "$DATA_FILE.tmp" "$DATA_FILE"
    chmod 644 "$DATA_FILE"
    sleep 1
done
