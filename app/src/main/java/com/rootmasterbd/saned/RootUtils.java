package com.rootmasterbd.saned;

import java.io.*;

public class RootUtils {
    public static final String MODDIR = "/data/adb/modules/android_optimization_module";
    public static final String DATA_FILE = "/data/local/tmp/android_perf_data";
    public static final String STATE_FILE = MODDIR + "/perf_state";
    public static final String APP_PREFS = "/data/local/tmp/bgguard_prefs";

    public static String runSu(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            p.waitFor();
            return sb.toString().trim();
        } catch (Exception e) { return "ERR:" + e.getMessage(); }
    }

    public static String readFile(String path) {
        return runSu("cat " + path + " 2>/dev/null");
    }

    public static void writeFile(String path, String content) {
        runSu("echo '" + content + "' > " + path);
    }

    public static String getCurrentMode() {
        String mode = readFile(STATE_FILE);
        return mode.isEmpty() ? "idle" : mode.trim();
    }

    public static void setMode(String mode) {
        writeFile(STATE_FILE, mode);
        runSu("sh " + MODDIR + "/performance_toggle.sh " + mode);
    }

    public static java.util.Map<String, String> readDataFile() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        try {
            String data = readFile(DATA_FILE);
            for (String line : data.split("\n")) {
                int idx = line.indexOf(':');
                if (idx > 0) map.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        } catch (Exception e) {}
        return map;
    }

    public static void savePref(String key, String value) {
        runSu("mkdir -p /data/local/tmp && sed -i '/^" + key + "=/d' " + APP_PREFS + " 2>/dev/null; echo '" + key + "=" + value + "' >> " + APP_PREFS);
    }

    public static String loadPref(String key, String def) {
        String result = runSu("grep '^" + key + "=' " + APP_PREFS + " 2>/dev/null | cut -d= -f2");
        return result.isEmpty() ? def : result.trim();
    }
}
