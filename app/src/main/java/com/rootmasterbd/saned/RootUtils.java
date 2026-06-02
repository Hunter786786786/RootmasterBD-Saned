package com.rootmasterbd.saned;
import java.io.*;
public class RootUtils {
    public static final String MODDIR = "/data/adb/modules/android_optimization_module";
    public static final String DATA_FILE = "/data/local/tmp/android_perf_data";
    public static final String STATE_FILE = MODDIR + "/perf_state";
    public static final String PREFS = "/data/local/tmp/rmbd_prefs";

    public static String su(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su","-c",cmd});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder(); String l;
            while((l=br.readLine())!=null) sb.append(l).append("\n");
            p.waitFor(); return sb.toString().trim();
        } catch(Exception e){ return ""; }
    }

    public static java.util.Map<String,String> readData() {
        java.util.Map<String,String> m = new java.util.HashMap<>();
        try {
            String d = su("cat " + DATA_FILE + " 2>/dev/null");
            for(String line : d.split("\n")) {
                int i = line.indexOf(':');
                if(i>0) m.put(line.substring(0,i).trim(), line.substring(i+1).trim());
            }
        } catch(Exception e){}
        return m;
    }

    public static void setMode(String mode) {
        su("echo " + mode + " > " + STATE_FILE);
        su("sh " + MODDIR + "/performance_toggle.sh " + mode + " &");
    }

    public static String getMode() {
        String m = su("cat " + STATE_FILE + " 2>/dev/null");
        return m.isEmpty() ? "idle" : m.trim();
    }

    public static void savePref(String key, String val) {
        su("mkdir -p /data/local/tmp && sed -i '/^" + key + "=/d' " + PREFS + " 2>/dev/null; echo '" + key + "=" + val + "' >> " + PREFS);
    }

    public static String loadPref(String key, String def) {
        String r = su("grep '^" + key + "=' " + PREFS + " 2>/dev/null | cut -d= -f2- | head -1");
        return r.isEmpty() ? def : r.trim();
    }

    // Check if app is running via proc scan
    public static String getAppStatus(String pkg) {
        // Foreground check
        String fg = su("dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp' | grep -c " + pkg);
        if("1".equals(fg.trim()) || fg.trim().matches("[2-9]|[0-9]{2,}")) return "Foreground";
        // Background check via proc
        String bg = su("for f in /proc/[0-9]*/cmdline; do cmd=$(cat $f 2>/dev/null | tr -d '\\0'); echo $cmd | grep -q '^" + pkg + "' && echo running && break; done");
        if(bg.contains("running")) return "Background";
        return "Inactive";
    }
}
