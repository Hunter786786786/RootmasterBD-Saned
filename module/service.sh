#!/system/bin/sh

MODDIR=${0%/*}

log_print() {
  echo "Android Optimization Module: $1" > /dev/kmsg
}

log_print "Starting service..."

until [ "$(getprop sys.boot_completed)" = "1" ]; do
  sleep 1
done

log_print "Boot completed. Starting daemons."

sh $MODDIR/process_priority.sh &
sh $MODDIR/network_opt.sh &
sh $MODDIR/monitor_daemon.sh &
sh $MODDIR/wakelock_guard.sh &
sh $MODDIR/app_keepalive.sh &
sh $MODDIR/gps_lock.sh &

log_print "All daemons started. Use Action button to toggle mode."
