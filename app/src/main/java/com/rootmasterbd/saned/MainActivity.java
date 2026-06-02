package com.rootmasterbd.saned;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private TextView tvMode, tvOryon, tvJahez, tvSaned, tvMs, tvLog;
    private Button btnActive, btnBalanced;
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
        root.setPadding(16,16,16,16);

        // Header
        TextView hdr = new TextView(this);
        hdr.setText("RootmasterBD Saned");
        hdr.setTextColor(Color.parseColor("#00B4FF"));
        hdr.setTextSize(20); hdr.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(hdr);

        tvMode = addText(root, "[BALANCED]", "#FFD700", 14);

        // Mode buttons
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(0,8,0,8);

        btnActive = new Button(this);
        btnActive.setText("ACTIVE");
        btnActive.setBackgroundColor(Color.parseColor("#0A1628"));
        btnActive.setTextColor(Color.parseColor("#00FF88"));
        btnActive.setOnClickListener(v -> setMode("active"));

        btnBalanced = new Button(this);
        btnBalanced.setText("BALANCED");
        btnBalanced.setBackgroundColor(Color.parseColor("#00FF88"));
        btnBalanced.setTextColor(Color.BLACK);
        btnBalanced.setOnClickListener(v -> setMode("idle"));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(0,0,4,0);
        btns.addView(btnActive, lp);
        btns.addView(btnBalanced, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(btns);

        // Stats
        addSep(root);
        tvOryon = addText(root, "Oryon: ---", "#FFD700", 13);
        tvJahez = addText(root, "Jahez: ---", "#90B8D8", 13);
        tvSaned = addText(root, "Saned: ---", "#90B8D8", 13);
        tvMs = addText(root, "Jahez MS: ---", "#90B8D8", 13);
        addSep(root);

        // Log
        TextView logHdr = new TextView(this);
        logHdr.setText("LIVE LOG"); logHdr.setTextColor(Color.parseColor("#4070A0")); logHdr.setTextSize(10);
        root.addView(logHdr);
        tvLog = addText(root, "Starting...", "#00FF88", 11);

        sv.addView(root);
        setContentView(sv);

        startUpdates();
        addLog("RootmasterBD Saned Started");
    }

    private void setMode(String mode) {
        exec.execute(() -> {
            su("echo " + mode + " > " + MODDIR + "/perf_state");
            su("sh " + MODDIR + "/performance_toggle.sh " + mode);
            addLog("Mode → " + mode.toUpperCase());
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
            Process p = Runtime.getRuntime().exec(new String[]{"su","-c","cat /data/local/tmp/android_perf_data"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String l;
            while((l=br.readLine())!=null){int i=l.indexOf(':');if(i>0)m.put(l.substring(0,i).trim(),l.substring(i+1).trim());}
        } catch(Exception e){}
        return m;
    }

    private void updateUI(Map<String,String> d) {
        String mode = d.getOrDefault("MODE","idle");
        tvMode.setText("[" + mode.toUpperCase() + "]");
        tvMode.setTextColor(Color.parseColor("active".equals(mode)?"#00FF88":"#FFD700"));

        String o = d.getOrDefault("ORYON","0|0");
        String[] op = o.split("\\|");
        tvOryon.setText("Oryon: " + (op.length>0?op[0]:"0") + "MHz | " + (op.length>1?op[1]:"0") + "MHz");

        String j = d.getOrDefault("JAHEZ","Inactive");
        tvJahez.setText("Jahez: " + j);
        tvJahez.setTextColor(Color.parseColor("Foreground".equals(j)?"#00FF88":"Inactive".equals(j)?"#FF4060":"#FFD700"));

        String s = d.getOrDefault("SANED","Inactive");
        tvSaned.setText("Saned: " + s);
        tvSaned.setTextColor(Color.parseColor("Inactive".equals(s)?"#FF4060":"#00FF88"));

        tvMs.setText("Jahez MS: " + d.getOrDefault("MS_JAHEZ", d.getOrDefault("MS","N/A")));
    }

    private TextView addText(LinearLayout p, String t, String c, float s) {
        TextView tv = new TextView(this);
        tv.setText(t); tv.setTextColor(Color.parseColor(c)); tv.setTextSize(s);
        p.addView(tv); return tv;
    }

    private void addSep(LinearLayout p) {
        View v = new View(this);
        v.setBackgroundColor(Color.parseColor("#152035"));
        p.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private void addLog(String msg) {
        handler.post(() -> {
            String cur = tvLog.getText().toString();
            String[] lines = cur.split("\n");
            StringBuilder sb = new StringBuilder();
            int start = lines.length > 20 ? lines.length - 20 : 0;
            for(int i=start;i<lines.length;i++) sb.append(lines[i]).append("\n");
            sb.append(msg);
            tvLog.setText(sb.toString());
        });
    }

    private String su(String cmd) {
        try { Process p = Runtime.getRuntime().exec(new String[]{"su","-c",cmd}); p.waitFor(); return "ok"; }
        catch(Exception e){ return "err"; }
    }

    @Override protected void onDestroy() { super.onDestroy(); handler.removeCallbacks(updateTask); exec.shutdown(); }
}
