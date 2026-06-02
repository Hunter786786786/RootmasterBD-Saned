#!/system/bin/sh
SKIPUNZIP=1

ui_print ""
ui_print "╔══════════════════════════════════════╗"
ui_print "║      RootmasterBD Saned v40          ║"
ui_print "║   Performance Optimizer for Jahez    ║"
ui_print "╚══════════════════════════════════════╝"
ui_print ""
ui_print "- Device: $(getprop ro.product.model)"
ui_print "- Android: $(getprop ro.build.version.release)"
ui_print "- SoC: $(getprop ro.soc.model)"
ui_print ""

ui_print "- Extracting module files..."
unzip -o "$ZIPFILE" -x 'META-INF/*' -d "$MODPATH" >&2
set_perm_recursive "$MODPATH" root root 0755 0644
chmod +x "$MODPATH"/*.sh

ui_print "- Detecting device..."
sh "$MODPATH/device_detect.sh" > /dev/null 2>&1
DEVICE_TAG=$(grep "DEVICE_TAG" /data/local/tmp/rm_device_profile 2>/dev/null | cut -d= -f2)
ui_print "- Device: ${DEVICE_TAG:-Unknown}"

ui_print ""
ui_print "- Installing RootmasterBD Saned App..."
if [ -f "$MODPATH/RootmasterBD_Saned.apk" ]; then
    pm install -r "$MODPATH/RootmasterBD_Saned.apk" > /dev/null 2>&1
    [ $? -eq 0 ] && ui_print "- App installed!" || ui_print "- APK install failed"
    rm -f "$MODPATH/RootmasterBD_Saned.apk"
else
    ui_print "- APK not found (build from GitHub first)"
fi

cp "$MODPATH/setup.sh" /data/local/tmp/rmbd_setup.sh 2>/dev/null
chmod +x /data/local/tmp/rmbd_setup.sh

ui_print ""
ui_print "╔══════════════════════════════════════╗"
ui_print "║  Installation Complete!              ║"
ui_print "║  REBOOT → Open RootmasterBD Saned   ║"
ui_print "║  Grant ROOT when asked               ║"
ui_print "║                                      ║"
ui_print "║  Build APK via Termux:               ║"
ui_print "║  bash /data/local/tmp/rmbd_setup.sh  ║"
ui_print "╚══════════════════════════════════════╝"
