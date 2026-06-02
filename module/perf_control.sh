#!/system/bin/sh

MODDIR="/data/adb/modules/android_optimization_module"
STATE_FILE="$MODDIR/perf_state"
DATA_FILE="/data/local/tmp/android_perf_data"

GRN="\033[32m"; RED="\033[31m"; YLW="\033[33m"
CYN="\033[36m"; WHT="\033[97m"; RST="\033[0m"

activate_mode() {
    echo "active" > "$STATE_FILE"
    sh "$MODDIR/performance_toggle.sh" active 2>/dev/null
    clear
    printf "${GRN}========================================${RST}\n"
    printf "${WHT}   ACTIVE MODE ON — Performance Locked ${RST}\n"
    printf "${GRN}========================================${RST}\n"
    printf " CPU:     ${YLW}4300MHz${RST} | GPU: ${YLW}MAX${RST}\n"
    printf " Thermal: ${GRN}OFF${RST}    | 144Hz: ${GRN}ON${RST}\n"
    printf "${GRN}========================================${RST}\n"
    printf "\n Press ${WHT}Enter${RST} to go back to menu...\n"
    read dummy
}

idle_mode() {
    echo "idle" > "$STATE_FILE"
    sh "$MODDIR/performance_toggle.sh" idle 2>/dev/null
    clear
    printf "${YLW}========================================${RST}\n"
    printf "${WHT}   BALANCED MODE ON                    ${RST}\n"
    printf "${YLW}========================================${RST}\n"
    printf "\n Press ${WHT}Enter${RST} to go back to menu...\n"
    read dummy
}

show_monitor() {
    clear
    printf "${CYN}Monitor running... Press Ctrl+C to exit${RST}\n\n"
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
            printf " $(date +%H:%M:%S) | ${RED}Ctrl+C${RST} to exit\n"
        else
            echo "Waiting for daemon..."
        fi
        sleep 1
    done
}

# ── MAIN MENU LOOP ──────────────────────────────────
while true; do
    clear
    printf "${CYN}============================================${RST}\n"
    printf "${WHT}   RootmasterBD Performance Controller     ${RST}\n"
    printf "${CYN}============================================${RST}\n"
    CURRENT=$(cat "$STATE_FILE" 2>/dev/null || echo "idle")
    [ "$CURRENT" = "active" ] && printf " Status: ${GRN}ACTIVE${RST}\n" || printf " Status: ${YLW}BALANCED${RST}\n"
    printf "${CYN}--------------------------------------------${RST}\n"
    printf " ${GRN}1${RST} → ACTIVE mode ON\n"
    printf " ${YLW}2${RST} → BALANCED mode\n"
    printf " ${CYN}3${RST} → Monitor dashboard\n"
    printf " ${RED}0${RST} → Exit\n"
    printf "${CYN}============================================${RST}\n"
    printf "Select (1/2/3/0): "
    read choice
    case "$choice" in
        1) activate_mode ;;
        2) idle_mode ;;
        3) show_monitor ;;
        0) exit 0 ;;
        *) printf "${RED}Invalid!${RST}\n"; sleep 1 ;;
    esac
done
