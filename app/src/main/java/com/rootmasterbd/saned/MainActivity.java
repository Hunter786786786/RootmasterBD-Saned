package com.rootmasterbd.saned;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private TextView tvMode, navHome, navApps, navTweaks, navSettings;
    private FrameLayout pageContainer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService exec = Executors.newFixedThreadPool(2);
    private int currentPage = 0;
    private HomeFragment homeFrag;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        tvMode = findViewById(R.id.tvMode);
        pageContainer = findViewById(R.id.pageContainer);
        navHome = findViewById(R.id.navHome);
        navApps = findViewById(R.id.navApps);
        navTweaks = findViewById(R.id.navTweaks);
        navSettings = findViewById(R.id.navSettings);

        navHome.setOnClickListener(v -> showPage(0));
        navApps.setOnClickListener(v -> showPage(1));
        navTweaks.setOnClickListener(v -> showPage(2));
        navSettings.setOnClickListener(v -> showPage(3));

        showPage(0);
        startModeUpdate();
    }

    private void showPage(int page) {
        currentPage = page;
        navHome.setTextColor(Color.parseColor(page==0?"#00B4FF":"#6080A0"));
        navApps.setTextColor(Color.parseColor(page==1?"#00B4FF":"#6080A0"));
        navTweaks.setTextColor(Color.parseColor(page==2?"#00B4FF":"#6080A0"));
        navSettings.setTextColor(Color.parseColor(page==3?"#00B4FF":"#6080A0"));

        pageContainer.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        switch(page) {
            case 0:
                View homeView = inf.inflate(R.layout.fragment_home, pageContainer, false);
                homeFrag = new HomeFragment(homeView, handler, exec, tvMode);
                pageContainer.addView(homeView);
                break;
            case 1:
                pageContainer.addView(new AppsPage(this, exec, handler));
                break;
            case 2:
                View tweaksView = inf.inflate(R.layout.fragment_tweaks, pageContainer, false);
                new TweaksPage(tweaksView, exec, handler);
                pageContainer.addView(tweaksView);
                break;
            case 3:
                View settingsView = inf.inflate(R.layout.fragment_settings, pageContainer, false);
                new SettingsPage(this, settingsView, exec, handler);
                pageContainer.addView(settingsView);
                break;
        }
    }

    private void startModeUpdate() {
        Runnable r = new Runnable() {
            public void run() {
                exec.execute(() -> {
                    String mode = RootUtils.getMode();
                    handler.post(() -> {
                        tvMode.setText("[" + mode.toUpperCase() + "]");
                        tvMode.setTextColor(Color.parseColor("active".equals(mode)||"gaming".equals(mode)?"#00FF88":"#FFD700"));
                    });
                });
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(r);
    }

    @Override protected void onDestroy() { super.onDestroy(); exec.shutdown(); }
}
