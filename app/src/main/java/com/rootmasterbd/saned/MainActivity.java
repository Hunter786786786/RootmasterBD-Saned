package com.rootmasterbd.saned;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private TextView tvMode, navHome, navApps, navTweaks, navSettings;
    private FrameLayout pageContainer;
    public Handler handler = new Handler(Looper.getMainLooper());
    public ExecutorService exec = Executors.newFixedThreadPool(3);
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
    }

    void showPage(int p) {
        int[] colors = {p==0?0xFF00B4FF:0xFF6080A0, p==1?0xFF00B4FF:0xFF6080A0, p==2?0xFF00B4FF:0xFF6080A0, p==3?0xFF00B4FF:0xFF6080A0};
        navHome.setTextColor(colors[0]); navApps.setTextColor(colors[1]);
        navTweaks.setTextColor(colors[2]); navSettings.setTextColor(colors[3]);
        pageContainer.removeAllViews();
        if(homeFrag != null) homeFrag.stop();
        LayoutInflater inf = LayoutInflater.from(this);
        switch(p) {
            case 0:
                View hv = inf.inflate(R.layout.fragment_home, pageContainer, false);
                homeFrag = new HomeFragment(hv, handler, exec, tvMode);
                pageContainer.addView(hv); break;
            case 1:
                pageContainer.addView(new AppsPage(this, exec, handler)); break;
            case 2:
                View tv = inf.inflate(R.layout.fragment_tweaks, pageContainer, false);
                new TweaksPage(tv, exec, handler); pageContainer.addView(tv); break;
            case 3:
                View sv = inf.inflate(R.layout.fragment_settings, pageContainer, false);
                new SettingsPage(this, sv, exec, handler); pageContainer.addView(sv); break;
        }
    }

    @Override protected void onDestroy() { super.onDestroy(); exec.shutdown(); }
}
