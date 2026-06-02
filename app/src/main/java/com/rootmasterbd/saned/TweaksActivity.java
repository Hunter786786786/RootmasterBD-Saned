package com.rootmasterbd.saned;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TweaksActivity extends Activity {

    private Switch swGamingMode, swGamingDns, swAppPriority, sw144fps;
    private Switch swNetworkOpt, swThermal, swWakelock, swGpsLock;
    private Button btnApply;
    private TextView tvStatus;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweaks);

        swGamingMode = findViewById(R.id.swGamingMode);
        swGamingDns = findViewById(R.id.swGamingDns);
        swAppPriority = findViewById(R.id.swAppPriority);
        sw144fps = findViewById(R.id.sw144fps);
        swNetworkOpt = findViewById(R.id.swNetworkOpt);
        swThermal = findViewById(R.id.swThermal);
        swWakelock = findViewById(R.id.swWakelock);
        swGpsLock = findViewById(R.id.swGpsLock);
        btnApply = findViewById(R.id.btnApplyTweaks);
        tvStatus = findViewById(R.id.tvTweakStatus);

        loadSavedTweaks();

        btnApply.setOnClickListener(v -> applyTweaks());
    }

    private void loadSavedTweaks() {
        swGamingMode.setChecked("1".equals(RootUtils.loadPref("tw_gaming_mode", "1")));
        swGamingDns.setChecked("1".equals(RootUtils.loadPref("tw_gaming_dns", "1")));
        swAppPriority.setChecked("1".equals(RootUtils.loadPref("tw_app_priority", "1")));
        sw144fps.setChecked("1".equals(RootUtils.loadPref("tw_144fps", "1")));
        swNetworkOpt.setChecked("1".equals(RootUtils.loadPref("tw_network", "1")));
        swThermal.setChecked("1".equals(RootUtils.loadPref("tw_thermal", "1")));
        swWakelock.setChecked("1".equals(RootUtils.loadPref("tw_wakelock", "1")));
        swGpsLock.setChecked("1".equals(RootUtils.loadPref("tw_gps", "1")));
    }

    private void applyTweaks() {
        tvStatus.setText("Applying tweaks...");
        tvStatus.setTextColor(Color.parseColor("#FFD700"));

        executor.execute(() -> {
            // Save prefs
            RootUtils.savePref("tw_gaming_mode", swGamingMode.isChecked() ? "1" : "0");
            RootUtils.savePref("tw_gaming_dns", swGamingDns.isChecked() ? "1" : "0");
            RootUtils.savePref("tw_app_priority", swAppPriority.isChecked() ? "1" : "0");
            RootUtils.savePref("tw_144fps", sw144fps.isChecked() ? "1" : "0");
            RootUtils.savePref("tw_network", swNetworkOpt.isChecked() ? "1" : "0");
            RootUtils.savePref("tw_thermal", swThermal.isChecked() ? "1" : "0");
            RootUtils.savePref("tw_wakelock", swWakelock.isChecked() ? "1" : "0");
            RootUtils.savePref("tw_gps", swGpsLock.isChecked() ? "1" : "0");

            // Apply tweaks via module scripts
            if (swNetworkOpt.isChecked())
                RootUtils.runSu("sh " + RootUtils.MODDIR + "/network_opt.sh &");
            if (swGpsLock.isChecked())
                RootUtils.runSu("sh " + RootUtils.MODDIR + "/gps_lock.sh &");
            if (swWakelock.isChecked())
                RootUtils.runSu("sh " + RootUtils.MODDIR + "/wakelock_guard.sh &");
            if (swAppPriority.isChecked())
                RootUtils.runSu("sh " + RootUtils.MODDIR + "/app_keepalive.sh &");
            if (sw144fps.isChecked()) {
                RootUtils.runSu("setprop persist.sys.display.refresh_rate 144");
                RootUtils.runSu("setprop persist.vendor.display.refresh_rate 144");
            }
            if (swGamingDns.isChecked()) {
                RootUtils.runSu("setprop net.dns1 1.1.1.1");
                RootUtils.runSu("setprop net.dns2 1.0.0.1");
            }
            if (swThermal.isChecked()) {
                RootUtils.runSu("for tz in /sys/devices/virtual/thermal/thermal_zone*/trip_point_0_temp; do echo 95000 > $tz 2>/dev/null; done");
            }

            handler.post(() -> {
                tvStatus.setText("✓ All tweaks applied!");
                tvStatus.setTextColor(Color.parseColor("#00FF88"));
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
