#!/system/bin/sh

# Universal UID finder — 6 methods for Android 16 compatibility
get_uid() {
    local pkg=$1
    local uid=""

    # Method 1: stat on app data directory
    uid=$(stat -c '%u' /data/data/"$pkg" 2>/dev/null)
    [ -n "$uid" ] && [ "$uid" -gt 999 ] && echo "$uid" && return

    # Method 2: ls -n
    uid=$(ls -ln /data/data/"$pkg" 2>/dev/null | head -1 | awk '{print $3}')
    [ -n "$uid" ] && [ "$uid" -gt 999 ] && echo "$uid" && return

    # Method 3: /proc/pid/status
    for pid in $(pgrep -f "$pkg" 2>/dev/null | head -5); do
        uid=$(grep "^Uid:" /proc/$pid/status 2>/dev/null | awk '{print $2}')
        [ -n "$uid" ] && [ "$uid" -gt 999 ] && echo "$uid" && return
    done

    # Method 4: dumpsys package
    uid=$(dumpsys package "$pkg" 2>/dev/null | grep "userId=" | head -1 | grep -oE "[0-9]+" | head -1)
    [ -n "$uid" ] && [ "$uid" -gt 999 ] && echo "$uid" && return

    # Method 5: pm dump
    uid=$(pm dump "$pkg" 2>/dev/null | grep "userId=" | head -1 | grep -oE "[0-9]+" | head -1)
    [ -n "$uid" ] && [ "$uid" -gt 999 ] && echo "$uid" && return

    # Method 6: find in /data/data permissions
    uid=$(ls -lan /data/data/ 2>/dev/null | grep "$pkg" | awk '{print $3}' | head -1)
    [ -n "$uid" ] && [ "$uid" -gt 999 ] && echo "$uid" && return

    echo ""
}

is_app_running() {
    local pkg=$1
    # Method 1: pgrep
    pgrep -f "$pkg" > /dev/null 2>&1 && return 0
    # Method 2: ps -A
    ps -A 2>/dev/null | grep -q "$pkg" && return 0
    # Method 3: pidof
    pidof "$pkg" > /dev/null 2>&1 && return 0
    # Method 4: proc cmdline scan
    for f in /proc/*/cmdline; do
        local cmd=$(cat "$f" 2>/dev/null | tr -d '\0')
        echo "$cmd" | grep -q "^${pkg}" && return 0
    done
    return 1
}

get_app_status() {
    local pkg=$1
    # Foreground check
    if dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp|mResumedActivity' | grep -q "$pkg"; then
        echo "Foreground"
        return
    fi
    # Running check
    if is_app_running "$pkg"; then
        echo "Background"
        return
    fi
    echo "Inactive"
}

[ -n "$1" ] && get_uid "$1"
