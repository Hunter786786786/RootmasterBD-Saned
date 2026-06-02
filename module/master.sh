#!/system/bin/sh

MODDIR="/data/adb/modules/android_optimization_module"
DATA_FILE="/data/local/tmp/android_perf_data"

GRN="\033[32m"; RED="\033[31m"; YLW="\033[33m"
CYN="\033[36m"; WHT="\033[97m"; RST="\033[0m"

run_monitor() {
    clear
    while true; do
        if [ -f "$DATA_FILE" ]; then
            MODE=$(grep "^MODE:" "$DATA_FILE" | cut -d: -f2)
            CPU=$(grep "^CPU:" "$DATA_FILE" | cut -d: -f2)
            GPU=$(grep "^GPU:" "$DATA_FILE" | cut -d: -f2)
            ORYON=$(grep "^ORYON:" "$DATA_FILE" | cut -d: -f2)
            JAHEZ=$(grep "^JAHEZ:" "$DATA_FILE" | cut -d: -f2)
            SANED=$(grep "^SANED:" "$DATA_FILE" | cut -d: -f2)
            MAPS=$(grep "^MAPS:" "$DATA_FILE" | cut -d: -f2)
            NET=$(grep "^NET:" "$DATA_FILE" | cut -d: -f2)
            MS_NET=$(grep "^MS_NET:" "$DATA_FILE" | cut -d: -f2)
            MS_JAHEZ=$(grep "^MS_JAHEZ:" "$DATA_FILE" | cut -d: -f2)
            HZ=$(grep "^HZ:" "$DATA_FILE" | cut -d: -f2)
            FPS=$(grep "^FPS:" "$DATA_FILE" | cut -d: -f2)
            WL=$(grep "^WAKELOCK:" "$DATA_FILE" | cut -d: -f2)
            GPS=$(grep "^GPS:" "$DATA_FILE" | cut -d: -f2)
            KA=$(grep "^KEEPALIVE:" "$DATA_FILE" | cut -d: -f2)
            BG=$(grep "^BGDATA:" "$DATA_FILE" | cut -d: -f2)
            IFACE=$(grep "^IFACE:" "$DATA_FILE" | cut -d: -f2)
            UID=$(grep "^UID:" "$DATA_FILE" | cut -d: -f2)

            O6=$(echo "$ORYON" | cut -d'|' -f1)
            O7=$(echo "$ORYON" | cut -d'|' -f2)
            DL=$(echo "$NET" | cut -d'|' -f1)
            UL=$(echo "$NET" | cut -d'|' -f2)
            [ "$MODE" = "active" ] && M_DISP="ACTIVE" || M_DISP="BALANCED"

            printf "\033[H"
            echo "==========================================="
            printf "   ${WHT}SYSTEM PERFORMANCE MONITOR v31${RST}\n"
            echo "==========================================="
            printf " Mode:    [${CYN}$M_DISP${RST}] | IF: ${WHT}${IFACE}${RST}\n"
            printf " CPU:     ${WHT}$CPU${RST} | GPU: ${WHT}$GPU%%${RST}\n"
            printf " Oryon:   ${YLW}${O6}MHz${RST} | ${YLW}${O7}MHz${RST}\n"
            printf " Refresh: ${WHT}${HZ}Hz${RST} | FPS: ${WHT}${FPS}${RST}\n"
            printf " UIDs:    J:${WHT}$(echo $UID|cut -d'|' -f1)${RST} S:${WHT}$(echo $UID|cut -d'|' -f2)${RST}\n"
            echo "-------------------------------------------"
            printf " Jahez: "
            [ "$JAHEZ" = "Foreground" ] && printf "${GRN}$JAHEZ${RST}\n" || { [ "$JAHEZ" = "Background" ] && printf "${YLW}$JAHEZ${RST}\n" || printf "${RED}$JAHEZ${RST}\n"; }
            printf " Saned: "
            [ "$SANED" = "Inactive" ] && printf "${RED}$SANED${RST}\n" || printf "${GRN}$SANED${RST}\n"
            printf " Maps:  "
            [ "$MAPS" = "Inactive" ] && printf "${RED}$MAPS${RST}\n" || printf "${GRN}$MAPS${RST}\n"
            echo "-------------------------------------------"
            printf " Net MS:   ${WHT}$MS_NET${RST}\n"
            printf " Jahez MS: "
            MS_VAL=$(echo $MS_JAHEZ | tr -d 'ms')
            if [ -n "$MS_VAL" ] && [ "$MS_VAL" -le 20 ] 2>/dev/null; then
                printf "${GRN}${MS_JAHEZ} (EXCELLENT)${RST}\n"
            elif [ -n "$MS_VAL" ] && [ "$MS_VAL" -le 50 ] 2>/dev/null; then
                printf "${YLW}${MS_JAHEZ} (GOOD)${RST}\n"
            else
                printf "${RED}${MS_JAHEZ} (HIGH)${RST}\n"
            fi
            printf " DL: ${WHT}${DL} KB/s${RST} | UL: ${WHT}${UL} KB/s${RST}\n"
            echo "-------------------------------------------"
            printf " Wakelock: "; [ "$WL" = "ON" ] && printf "${GRN}ON${RST}\n" || printf "${RED}OFF${RST}\n"
            printf " GPS:      "; [ "$GPS" = "HIGH_ACC" ] && printf "${GRN}HIGH_ACC${RST}\n" || printf "${YLW}$GPS${RST}\n"
            printf " KeepAlive:"; [ "$KA" = "PROTECTED" ] && printf "${GRN}PROTECTED${RST}\n" || printf "${YLW}$KA${RST}\n"
            printf " BG Data:  "; [ "$BG" = "ALLOWED" ] && printf "${GRN}ALLOWED${RST}\n" || printf "${RED}$BG${RST}\n"
            echo "-------------------------------------------"
            printf " $(date +%H:%M:%S) | Ctrl+C to exit\n"
        else
            echo "Waiting for daemon..."
        fi
        sleep 1
    done
}

run_active() {
    echo "active" > "$MODDIR/perf_state"
    sh "$MODDIR/performance_toggle.sh" active 2>/dev/null
    printf "${GRN}[ACTIVE MODE ON]${RST}\n"
}

run_idle() {
    echo "idle" > "$MODDIR/perf_state"
    sh "$MODDIR/performance_toggle.sh" idle 2>/dev/null
    printf "${YLW}[BALANCED MODE ON]${RST}\n"
}

case "$1" in
    monitor) run_monitor ;;
    toggle)  sh "$MODDIR/performance_toggle.sh" ;;
    active)  run_active ;;
    idle)    run_idle ;;
    volkey)  sh "$MODDIR/perf_control.sh" ;;
    *)
        printf "${CYN}RootmasterBD Performance Controller v31${RST}\n"
        printf "Commands:\n"
        printf "  ${WHT}monitor${RST} — Live dashboard\n"
        printf "  ${WHT}active${RST}  — Force ACTIVE mode\n"
        printf "  ${WHT}idle${RST}    — Force BALANCED mode\n"
        printf "  ${WHT}volkey${RST}  — Volume button control\n"
        printf "  ${WHT}toggle${RST}  — Toggle mode\n\n"
        printf "Usage: su -c sh $MODDIR/master.sh [command]\n"
        ;;
esac
