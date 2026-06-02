package com.rootmasterbd.saned;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends Activity {

    private Switch swAutoSwitch, swAllAppKill;
    private EditText etKillWhitelist;
    private Button btnSaveSettings, btnKillNow;
    private TextView tvSettingsStatus;
    private Spinner spDefaultMode;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        swAutoSwitch = findViewById(R.id.swAutoSwitch);
        swAllAppKill = findViewById(R.id.swAllAppKill);
        etKillWhitelist = findViewById(R.id.etKillWhitelist);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnKillNow = findViewById(R.id.btnKillNow);
        tvSettingsStatus = findViewById(R.id.tvSettingsStatus);
        spDefaultMode = findViewById(R.id.spDefaultMode);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item,
            new String[]{"balanced", "performance", "gaming"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDefaultMode.setAdapter(adapter);

        loadSettings();

        btnSaveSettings.setOnClickListener(v -> saveSettings());
        btnKillNow.setOnClickListener(v -> killAppsNow());
    }

    private void loadSettings() {
        swAutoSwitch.setChecked("1".equals(RootUtils.loadPref("auto_switch", "0")));
        swAllAppKill.setChecked("1".equals(RootUtils.loadPref("all_app_kill", "0")));
        String whitelist = RootUtils.loadPref("kill_whitelist",
            "net.jahez.fleets io.suqi8.saned com.google.android.apps.maps com.termux");
        etKillWhitelist.setText(whitelist);

        String defMode = RootUtils.loadPref("default_mode", "balanced");
        int idx = defMode.equals("performance") ? 1 : defMode.equals("gaming") ? 2 : 0;
        spDefaultMode.setSelection(idx);
    }

    private void saveSettings() {
        RootUtils.savePref("auto_switch", swAutoSwitch.isChecked() ? "1" : "0");
        RootUtils.savePref("all_app_kill", swAllAppKill.isChecked() ? "1" : "0");
        RootUtils.savePref("kill_whitelist", etKillWhitelist.getText().toString().trim());
        String[] modes = {"balanced", "performance", "gaming"};
        RootUtils.savePref("default_mode", modes[spDefaultMode.getSelectedItemPosition()]);
        tvSettingsStatus.setText("✓ Settings Saved!");
        tvSettingsStatus.setTextColor(Color.parseColor("#00FF88"));
    }

    private void killAppsNow() {
        tvSettingsStatus.setText("Killing background apps...");
        tvSettingsStatus.setTextColor(Color.parseColor("#FFD700"));
        executor.execute(() -> {
            String whitelist = etKillWhitelist.getText().toString().trim();
            String[] whiteApps = whitelist.split("\\s+");
            StringBuilder grepExclude = new StringBuilder();
            for (String pkg : whiteApps) {
                if (grepExclude.length() > 0) grepExclude.append("|");
                grepExclude.append(pkg);
            }
            String killCmd = "for pid in $(ls /proc/ | grep -E '^[0-9]+$'); do " +
                "pkg=$(cat /proc/$pid/cmdline 2>/dev/null | tr -d '\\0' | cut -d: -f1); " +
                "echo \"$pkg\" | grep -q '\\.' || continue; " +
                "echo \"$pkg\" | grep -qE '" + grepExclude + "|system|zygote|android|com.google.android.gms' && continue; " +
                "kill -9 $pid 2>/dev/null; done; echo DONE";
            RootUtils.runSu(killCmd);
            handler.post(() -> {
                tvSettingsStatus.setText("✓ Background apps killed!");
                tvSettingsStatus.setTextColor(Color.parseColor("#00FF88"));
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
