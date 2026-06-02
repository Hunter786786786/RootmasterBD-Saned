#!/usr/bin/bash

# Termux Telemetry Viewer
# Usage: Run this in Termux (requires root or access to /dev/android_perf_data)

# Use a more reliable path for data exchange between root and Termux
DATA_FILE="/data/local/tmp/android_perf_data"

clear
echo "==========================================="
echo "   Android System Performance Monitor      "
echo "==========================================="

while true; do
    if [ -f "$DATA_FILE" ]; then
        DATA=$(cat "$DATA_FILE")
        
        CPU=$(echo $DATA | cut -d'|' -f1 | cut -d':' -f2)
        GPU=$(echo $DATA | cut -d'|' -f2 | cut -d':' -f2)
        JAHEZ=$(echo $DATA | cut -d'|' -f3 | cut -d':' -f2)
        SANED=$(echo $DATA | cut -d'|' -f4 | cut -d':' -f2)
        MODE=$(echo $DATA | cut -d'|' -f5 | cut -d':' -f2)
        
        if [ "$MODE" = "active" ]; then
            MODE_DISP="ACTIVE (AUTO)"
        else
            MODE_DISP="BALANCED (AUTO)"
        fi

        tput cup 4 0
        echo "System Mode:  $MODE_DISP      "
        echo "CPU Load:     $CPU          "
        echo "GPU Load:     $GPU%         "
        echo "-------------------------------------------"
        echo "Jahez App:    $JAHEZ        "
        echo "Saned App:    $SANED        "
        echo "-------------------------------------------"
        echo "Last Update:  $(date +%H:%M:%S) "
    else
        echo "Waiting for data from module..."
    fi
    sleep 2
done
