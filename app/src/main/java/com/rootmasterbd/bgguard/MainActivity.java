package com.rootmasterbd.bgguard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    // Mode constants
    public static final String MODE_BALANCED = "idle";
    public static final String MODE_PERFORMANCE = "active";
    public static final String MODE_GAMING = "gaming";

    private TextView tvMode, tvOryon, tvCpu, tvGpu, tvRefresh, tvFps;
    private TextView tvJahez, tvSaned, tvMaps;
    private TextView tvNetMs, tvJahezMs, tvDl, tvUl;
    private TextView tvWakelock, tvGps, tvKeepalive, tvBgdata;
    private TextView tvUid, tvIface;
    private Button btnBalanced, btnPerformance, btnGaming;
    private LinearLayout logContainer;
    private ScrollView scrollLog;

    private Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService executor = Executors.newFixedThreadPool(3);
    private Runnable monitorRunnable;
    private String currentMode = MODE_BALANCED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        loadCurrentMode();
        startMonitor();
        addLog("BGGuard Started", "#00FF88");
    }

    private void initViews() {
        tvMode = findViewById(R.id.tvMode);
        tvOryon = findViewById(R.id.tvOryon);
        tvCpu = findViewById(R.id.tvCpu);
        tvGpu = findViewById(R.id.tvGpu);
        tvRefresh = findViewById(R.id.tvRefresh);
        tvFps = findViewById(R.id.tvFps);
        tvJahez = findViewById(R.id.tvJahez);
        tvSaned = findViewById(R.id.tvSaned);
        tvMaps = findViewById(R.id.tvMaps);
        tvNetMs = findViewById(R.id.tvNetMs);
        tvJahezMs = findViewById(R.id.tvJahezMs);
        tvDl = findViewById(R.id.tvDl);
        tvUl = findViewById(R.id.tvUl);
        tvWakelock = findViewById(R.id.tvWakelock);
        tvGps = findViewById(R.id.tvGps);
        tvKeepalive = findViewById(R.id.tvKeepalive);
        tvBgdata = findViewById(R.id.tvBgdata);
        tvUid = findViewById(R.id.tvUid);
        tvIface = findViewById(R.id.tvIface);
        btnBalanced = findViewById(R.id.btnBalanced);
        btnPerformance = findViewById(R.id.btnPerformance);
        btnGaming = findViewById(R.id.btnGaming);
        scrollLog = findViewById(R.id.scrollLog);
        logContainer = findViewById(R.id.logContainer);

        btnBalanced.setOnClickListener(v -> setMode(MODE_BALANCED));
        btnPerformance.setOnClickListener(v -> setMode(MODE_PERFORMANCE));
        btnGaming.setOnClickListener(v -> setMode(MODE_GAMING));

        findViewById(R.id.btnAppList).setOnClickListener(v ->
            startActivity(new Intent(this, AppListActivity.class)));
        findViewById(R.id.btnTweaks).setOnClickListener(v ->
            startActivity(new Intent(this, TweaksActivity.class)));
        findViewById(R.id.btnSettings).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void loadCurrentMode() {
        executor.execute(() -> {
            currentMode = RootUtils.getCurrentMode();
            handler.post(() -> updateModeButtons(currentMode));
        });
    }

    private void setMode(String mode) {
        currentMode = mode;
        updateModeButtons(mode);
        executor.execute(() -> {
            if (mode.equals(MODE_GAMING)) {
                RootUtils.setMode(MODE_PERFORMANCE);
                RootUtils.runSu("sh " + RootUtils.MODDIR + "/perf_tuning.sh gaming");
                RootUtils.savePref("current_mode", MODE_GAMING);
            } else {
                RootUtils.setMode(mode);
                RootUtils.savePref("current_mode", mode);
            }
            addLog("Mode → " + modeName(mode), modeColor(mode));
        });
    }

    private void updateModeButtons(String mode) {
        btnBalanced.setBackgroundColor(Color.parseColor(mode.equals(MODE_BALANCED) ? "#00FF88" : "#0A1628"));
        btnBalanced.setTextColor(Color.parseColor(mode.equals(MODE_BALANCED) ? "#000000" : "#00FF88"));
        btnPerformance.setBackgroundColor(Color.parseColor(mode.equals(MODE_PERFORMANCE) ? "#00B4FF" : "#0A1628"));
        btnPerformance.setTextColor(Color.parseColor(mode.equals(MODE_PERFORMANCE) ? "#000000" : "#00B4FF"));
        btnGaming.setBackgroundColor(Color.parseColor(mode.equals(MODE_GAMING) ? "#FF8C00" : "#0A1628"));
        btnGaming.setTextColor(Color.parseColor(mode.equals(MODE_GAMING) ? "#000000" : "#FF8C00"));

        tvMode.setText("[" + modeName(mode) + "]");
        tvMode.setTextColor(Color.parseColor(modeColor(mode)));
    }

    private String modeName(String mode) {
        switch (mode) {
            case MODE_PERFORMANCE: return "PERFORMANCE";
            case MODE_GAMING: return "GAMING";
            default: return "BALANCED";
        }
    }

    private String modeColor(String mode) {
        switch (mode) {
            case MODE_PERFORMANCE: return "#00B4FF";
            case MODE_GAMING: return "#FF8C00";
            default: return "#00FF88";
        }
    }

    private void startMonitor() {
        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                executor.execute(() -> {
                    Map<String, String> data = RootUtils.readDataFile();
                    handler.post(() -> updateUI(data));
                });
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(monitorRunnable);
    }

    private void updateUI(Map<String, String> d) {
        // Oryon
        String oryon = d.getOrDefault("ORYON", "0|0");
        String[] op = oryon.split("\\|");
        int o1 = safeInt(op.length > 0 ? op[0] : "0");
        tvOryon.setText(o1 + "MHz | " + (op.length > 1 ? op[1] : "0") + "MHz");
        tvOryon.setTextColor(Color.parseColor(o1 >= 4000 ? "#00FF88" : o1 >= 3000 ? "#FFD700" : "#FF6B6B"));

        tvCpu.setText("CPU: " + d.getOrDefault("CPU", "N/A"));
        tvGpu.setText("GPU: " + d.getOrDefault("GPU", "0") + "%");
        tvRefresh.setText(d.getOrDefault("HZ", "144") + "Hz");
        tvFps.setText("FPS: " + d.getOrDefault("FPS", "N/A"));
        tvIface.setText("IF: " + d.getOrDefault("IFACE", "?"));

        String uid = d.getOrDefault("UID", "?|?");
        String[] up = uid.split("\\|");
        tvUid.setText("J:" + (up.length > 0 ? up[0] : "?") + " S:" + (up.length > 1 ? up[1] : "?"));

        setAppStatus(tvJahez, "Jahez", d.getOrDefault("JAHEZ", "Inactive"));
        setAppStatus(tvSaned, "Saned", d.getOrDefault("SANED", "Inactive"));
        setAppStatus(tvMaps, "Maps", d.getOrDefault("MAPS", "Inactive"));

        tvNetMs.setText("Net: " + d.getOrDefault("MS_NET", d.getOrDefault("MS", "N/A")));
        String msJahez = d.getOrDefault("MS_JAHEZ", "N/A");
        tvJahezMs.setText("Jahez MS: " + msJahez);
        int msVal = safeInt(msJahez.replace("ms", "").trim());
        tvJahezMs.setTextColor(Color.parseColor(msVal > 0 && msVal <= 20 ? "#00FF88" : msVal <= 50 ? "#FFD700" : "#FF6B6B"));

        String net = d.getOrDefault("NET", "0|0");
        String[] np = net.split("\\|");
        tvDl.setText("↓ " + (np.length > 0 ? np[0] : "0") + " KB/s");
        tvUl.setText("↑ " + (np.length > 1 ? np[1] : "0") + " KB/s");

        setStatus(tvWakelock, "Wakelock", d.getOrDefault("WAKELOCK", "OFF"), "ON");
        setStatus(tvGps, "GPS", d.getOrDefault("GPS", "OFF"), "HIGH_ACC");
        setStatus(tvKeepalive, "KA", d.getOrDefault("KEEPALIVE", "N/A"), "PROTECTED");
        setStatus(tvBgdata, "BG", d.getOrDefault("BGDATA", "N/A"), "ALLOWED");

        if (o1 > 0 && o1 < 3000 && !currentMode.equals(MODE_BALANCED))
            addLog("⚠ CPU throttle: " + o1 + "MHz", "#FFD700");
        if (msVal > 100 && msVal > 0)
            addLog("⚠ High MS: " + msJahez, "#FF6B6B");
    }

    private void setAppStatus(TextView tv, String label, String status) {
        tv.setText(label + ": " + status);
        tv.setTextColor(Color.parseColor(
            "Foreground".equals(status) ? "#00FF88" :
            "Background".equals(status) ? "#FFD700" : "#FF6B6B"));
    }

    private void setStatus(TextView tv, String label, String val, String good) {
        tv.setText(label + ": " + val);
        tv.setTextColor(Color.parseColor(val.equals(good) ? "#00FF88" : "#FF6B6B"));
    }

    public void addLog(String msg, String color) {
        handler.post(() -> {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            TextView log = new TextView(this);
            log.setText("[" + time + "] " + msg);
            log.setTextColor(Color.parseColor(color));
            log.setTextSize(11f);
            log.setPadding(4, 1, 4, 1);
            logContainer.addView(log);
            if (logContainer.getChildCount() > 80) logContainer.removeViewAt(0);
            scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
        });
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (monitorRunnable != null) handler.removeCallbacks(monitorRunnable);
        executor.shutdown();
    }
}
