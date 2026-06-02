package com.rootmasterbd.saned;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import android.view.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private TextView tvMode, tvOryon, tvJahez, tvSaned, tvMs, tvLog;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService exec = Executors.newSingleThreadExecutor();
    private static final String MODDIR = "/data/adb/modules/android_optimization_module";
    private Runnable updateTask;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#050A0F"));
        root.setPadding(20, 20, 20, 20);

        // Header
        TextView hdr = mkTv("RootmasterBD Saned", "#00B4FF", 20, true);
        root.addView(hdr);
        tvMode = mkTv("[BALANCED]", "#FFD700", 14, true);
        root.addView(tvMode);

        // Buttons
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(0, 12, 0, 12);
        Button btnA = mkBtn("ACTIVE", "#00FF88", "#000000");
        Button btnB = mkBtn("BALANCED", "#0A1628", "#00FF88");
        Button btnG = mkBtn("GAMING", "#0A1628", "#FF8C00");
        btnA.setOnClickListener(v -> setMode("active", "ACTIVE"));
        btnB.setOnClickListener(v -> setMode("idle", "BALANCED"));
        btnG.setOnClickListener(v -> setMode("active", "GAMING"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(0,0,4,0);
        btns.addView(btnA, lp);
        btns.addView(btnB, lp);
        btns.addView(btnG, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(btns);

        // Divider
        root.addView(mkDiv());
        root.addView(mkTv("PERFORMANCE", "#4070A0", 10, false));
        tvOryon = mkTv("Oryon: ---", "#FFD700", 15, true);
        root.addView(tvOryon);

        root.addView(mkDiv());
        root.addView(mkTv("TARGET APPS", "#4070A0", 10, false));
        tvJahez = mkTv("Jahez: ---", "#90B8D8", 13, false);
        tvSaned = mkTv("Saned: ---", "#90B8D8", 13, false);
        root.addView(tvJahez);
        root.addView(tvSaned);

        root.addView(mkDiv());
        root.addView(mkTv("NETWORK", "#4070A0", 10, false));
        tvMs = mkTv("Jahez MS: ---", "#90B8D8", 14, true);
        root.addView(tvMs);

        root.addView(mkDiv());
        root.addView(mkTv("LIVE LOG", "#4070A0", 10, false));
        tvLog = mkTv("Starting...", "#00FF88", 11, false);
        root.addView(tvLog);

        sv.addView(root);
        setContentView(sv);
        startUpdates();
        log("RootmasterBD Saned v1.0 Started");
    }

    private void setMode(String mode, String label) {
        exec.execute(() -> {
            su("echo " + mode + " > " + MODDIR + "/perf_state");
            su("sh " + MODDIR + "/performance_toggle.sh " + mode);
            log("Mode → " + label);
        });
    }

    private void startUpdates() {
        updateTask = new Runnable() {
            public void run() {
                exec.execute(() -> {
                    Map<String,String> d = readData();
                    handler.post(() -> updateUI(d));
                });
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateTask);
    }

    private Map<String,String> readData() {
        Map<String,String> m = new HashMap<>();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su","-c","cat /data/local/tmp/android_perf_data 2>/dev/null"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String l;
            while ((l = br.readLine()) != null) {
                int i = l.indexOf(':');
                if (i > 0) m.put(l.substring(0, i).trim(), l.substring(i + 1).trim());
            }
        } catch (Exception e) {}
        return m;
    }

    private void updateUI(Map<String,String> d) {
        String mode = d.getOrDefault("MODE", "idle");
        tvMode.setText("[" + mode.toUpperCase() + "]");
        tvMode.setTextColor(Color.parseColor("active".equals(mode) ? "#00FF88" : "#FFD700"));

        String o = d.getOrDefault("ORYON", "0|0");
        String[] op = o.split("\\|");
        tvOryon.setText("Oryon: " + (op.length > 0 ? op[0] : "0") + "MHz | " + (op.length > 1 ? op[1] : "0") + "MHz");
        int ov = safeInt(op.length > 0 ? op[0] : "0");
        tvOryon.setTextColor(Color.parseColor(ov >= 4000 ? "#00FF88" : ov >= 3000 ? "#FFD700" : "#FF4060"));

        String j = d.getOrDefault("JAHEZ", "Inactive");
        tvJahez.setText("Jahez: " + j);
        tvJahez.setTextColor(Color.parseColor("Foreground".equals(j) ? "#00FF88" : "Background".equals(j) ? "#FFD700" : "#FF4060"));

        String s = d.getOrDefault("SANED", "Inactive");
        tvSaned.setText("Saned: " + s);
        tvSaned.setTextColor(Color.parseColor("Inactive".equals(s) ? "#FF4060" : "#00FF88"));

        String ms = d.getOrDefault("MS_JAHEZ", d.getOrDefault("MS", "N/A"));
        tvMs.setText("Jahez MS: " + ms);
        int mv = safeInt(ms.replace("ms","").trim());
        tvMs.setTextColor(Color.parseColor(mv > 0 && mv <= 20 ? "#00FF88" : mv <= 50 ? "#FFD700" : "#FF4060"));
    }

    private TextView mkTv(String text, String color, float size, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(color));
        tv.setTextSize(size);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 4, 0, 4);
        return tv;
    }

    private Button mkBtn(String text, String bg, String fg) {
        Button b = new Button(this);
        b.setText(text);
        b.setBackgroundColor(Color.parseColor(bg));
        b.setTextColor(Color.parseColor(fg));
        b.setTextSize(11);
        return b;
    }

    private View mkDiv() {
        View v = new View(this);
        v.setBackgroundColor(Color.parseColor("#152035"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, 8, 0, 8);
        v.setLayoutParams(lp);
        return v;
    }

    private void log(String msg) {
        handler.post(() -> {
            String cur = tvLog.getText().toString();
            String[] lines = cur.split("\n");
            StringBuilder sb = new StringBuilder();
            int s = lines.length > 30 ? lines.length - 30 : 0;
            for (int i = s; i < lines.length; i++) sb.append(lines[i]).append("\n");
            sb.append(msg);
            tvLog.setText(sb.toString());
        });
    }

    private String su(String cmd) {
        try { Process p = Runtime.getRuntime().exec(new String[]{"su","-c",cmd}); p.waitFor(); return "ok"; }
        catch (Exception e) { return "err"; }
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateTask != null) handler.removeCallbacks(updateTask);
        exec.shutdown();
    }
}
