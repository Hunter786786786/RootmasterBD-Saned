#!/system/bin/sh

MODDIR="/data/adb/modules/android_optimization_module"
PKG_JAHEZ="net.jahez.fleets"
PKG_SANED="io.suqi8.saned"

. "$MODDIR/uid_helper.sh"

log_print() {
  echo "Network King: $1" > /dev/kmsg
}

log_print "Starting Network Optimization..."

# TCP/IP Stack — Ultra Low Latency
echo "bbr" > /proc/sys/net/ipv4/tcp_congestion_control 2>/dev/null
echo "fq_codel" > /proc/sys/net/core/default_qdisc 2>/dev/null
echo "1" > /proc/sys/net/ipv4/tcp_low_latency 2>/dev/null
echo "3" > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null
echo "0" > /proc/sys/net/ipv4/tcp_timestamps 2>/dev/null
echo "1" > /proc/sys/net/ipv4/tcp_sack 2>/dev/null
echo "1" > /proc/sys/net/ipv4/tcp_window_scaling 2>/dev/null
echo "0" > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null
echo "1" > /proc/sys/net/ipv4/tcp_mtu_probing 2>/dev/null
echo "0" > /proc/sys/net/ipv4/tcp_autocorking 2>/dev/null
echo "10" > /proc/sys/net/ipv4/tcp_keepalive_time 2>/dev/null
echo "3" > /proc/sys/net/ipv4/tcp_keepalive_intvl 2>/dev/null
echo "3" > /proc/sys/net/ipv4/tcp_keepalive_probes 2>/dev/null
echo "5" > /proc/sys/net/ipv4/tcp_retries2 2>/dev/null
echo "2" > /proc/sys/net/ipv4/tcp_syn_retries 2>/dev/null
echo "2" > /proc/sys/net/ipv4/tcp_synack_retries 2>/dev/null
echo "16777216" > /proc/sys/net/core/rmem_max 2>/dev/null
echo "16777216" > /proc/sys/net/core/wmem_max 2>/dev/null
echo "4096 87380 16777216" > /proc/sys/net/ipv4/tcp_rmem 2>/dev/null
echo "4096 65536 16777216" > /proc/sys/net/ipv4/tcp_wmem 2>/dev/null
echo "10000" > /proc/sys/net/core/netdev_max_backlog 2>/dev/null

# DNS — Cloudflare fastest
setprop net.dns1 1.1.1.1
setprop net.dns2 1.0.0.1
setprop net.rmnet.dns1 1.1.1.1
setprop net.rmnet.dns2 1.0.0.1

# Mobile radio — always active, no sleep
setprop persist.radio.data_no_toggle 1 2>/dev/null
setprop persist.data.netmgrd.qos.enable 1 2>/dev/null
setprop net.rmnet.priority 1 2>/dev/null
setprop net.data.default.dormancy 0 2>/dev/null
setprop persist.radio.lte_vrte_limited 0 2>/dev/null
setprop persist.radio.force_on_dc true 2>/dev/null
setprop persist.radio.apm_sim_not_pwdn 1 2>/dev/null

# Wait for UIDs then allow unrestricted background data
WAIT=0
UID_JAHEZ=""
UID_SANED=""
while [ $WAIT -lt 60 ]; do
    UID_JAHEZ=$(get_uid $PKG_JAHEZ)
    UID_SANED=$(get_uid $PKG_SANED)
    [ -n "$UID_JAHEZ" ] && [ -n "$UID_SANED" ] && break
    sleep 2
    WAIT=$((WAIT + 2))
done

if [ -n "$UID_JAHEZ" ] && [ -n "$UID_SANED" ]; then
    echo "$UID_JAHEZ" > /data/local/tmp/uid_jahez
    echo "$UID_SANED" > /data/local/tmp/uid_saned
    chmod 644 /data/local/tmp/uid_jahez /data/local/tmp/uid_saned

    cmd appops set $PKG_JAHEZ RUN_IN_BACKGROUND allow 2>/dev/null
    cmd appops set $PKG_SANED RUN_IN_BACKGROUND allow 2>/dev/null
    cmd appops set $PKG_JAHEZ RUN_ANY_IN_BACKGROUND allow 2>/dev/null
    cmd appops set $PKG_SANED RUN_ANY_IN_BACKGROUND allow 2>/dev/null

    log_print "UIDs ready: J=$UID_JAHEZ S=$UID_SANED"
else
    log_print "UIDs not found — daemon will retry"
fi

log_print "Network Optimization Done."
