#!/system/bin/sh

PKG_JAHEZ="net.jahez.fleets"
PKG_SANED="io.suqi8.saned"
PKG_MAPS="com.google.android.apps.maps"

log_print() {
  echo "GPS_Lock: $1" > /dev/kmsg
}

log_print "GPS Lock Guard started."

# Force GPS always on — never turn off
setprop persist.sys.gps.onfirst 1 2>/dev/null
setprop persist.gps.started 1 2>/dev/null
setprop persist.sys.location.always 1 2>/dev/null

# High accuracy GPS mode
settings put secure location_mode 3 2>/dev/null
settings put secure location_providers_allowed +gps 2>/dev/null
settings put secure location_providers_allowed +network 2>/dev/null
settings put global assisted_gps_enabled 1 2>/dev/null

# GPS performance settings
setprop persist.sys.gps.accuracy high 2>/dev/null
setprop ro.ril.def.agps.mode 2 2>/dev/null
setprop ro.ril.def.agps.feature 1 2>/dev/null
setprop persist.sys.agps.enable 1 2>/dev/null

# Disable GPS power saving (prevents fade/loss)
setprop persist.sys.gps.power_save 0 2>/dev/null
setprop config.gps.power_down_on_stop 0 2>/dev/null

log_print "GPS base settings applied."

while true; do
    # Check if Jahez is running
    if pgrep -f "$PKG_JAHEZ" > /dev/null; then

        # Keep Google Maps GPS process alive
        for pid in $(pgrep -f "$PKG_MAPS" 2>/dev/null); do
            renice -n -10 -p $pid 2>/dev/null
            echo -900 > /proc/$pid/oom_score_adj 2>/dev/null
        done

        # Keep GPS daemon alive
        for gps_proc in gpsd loc_launcher location_manager; do
            for pid in $(pgrep -f "$gps_proc" 2>/dev/null); do
                renice -n -15 -p $pid 2>/dev/null
                echo -1000 > /proc/$pid/oom_score_adj 2>/dev/null
            done
        done

        # Force location services active
        settings put secure location_mode 3 2>/dev/null
        setprop persist.gps.started 1 2>/dev/null

        # GPS wakelock — prevent GPS from sleeping
        echo "GpsLockModule" > /sys/power/wake_lock 2>/dev/null

        # Prevent Maps from being killed by system
        dumpsys deviceidle whitelist +$PKG_MAPS 2>/dev/null

    else
        echo "GpsLockModule" > /sys/power/wake_unlock 2>/dev/null
    fi

    sleep 3
done
