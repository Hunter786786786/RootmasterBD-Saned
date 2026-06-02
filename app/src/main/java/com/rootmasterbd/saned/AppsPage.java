package com.rootmasterbd.saned;

import android.content.Context;
import android.content.pm.*;
import android.graphics.Color;
import android.os.Handler;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class AppsPage extends ScrollView {
    private LinearLayout container;
    private List<AppItem> allApps = new ArrayList<>();
    private ExecutorService exec;
    private Handler handler;
    private Context ctx;
    private Map<String,String> appModes = new HashMap<>();
    private Map<String,Boolean> appEnabled = new HashMap<>();

    static class AppItem {
        String label, pkg;
        AppItem(String l, String p){ label=l; pkg=p; }
    }

    public AppsPage(Context c, ExecutorService e, Handler h) {
        super(c); ctx=c; exec=e; handler=h;
        setBackgroundColor(Color.parseColor("#050A0F"));

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12,12,12,12);

        // Header
        TextView title = new TextView(c);
        title.setText("APP MODE ASSIGNMENT");
        title.setTextColor(Color.parseColor("#00B4FF"));
        title.setTextSize(14); title.setTypeface(null,android.graphics.Typeface.BOLD);
        title.setPadding(0,0,0,8); root.addView(title);

        TextView sub = new TextView(c);
        sub.setText("Tap app to enable and assign mode");
        sub.setTextColor(Color.parseColor("#6080A0")); sub.setTextSize(11);
        sub.setPadding(0,0,0,12); root.addView(sub);

        // Search
        EditText search = new EditText(c);
        search.setHint("Search..."); search.setHintTextColor(Color.parseColor("#405060"));
        search.setTextColor(Color.parseColor("#D0E8FF")); search.setBackgroundColor(Color.parseColor("#080E18"));
        search.setPadding(12,10,12,10);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0,0,0,8); search.setLayoutParams(slp);
        root.addView(search);

        container = new LinearLayout(c);
        container.setOrientation(LinearLayout.VERTICAL);
        root.addView(container);
        addView(root);

        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int i,int cc,int a){}
            public void onTextChanged(CharSequence s,int i,int b,int cc){filter(s.toString());}
            public void afterTextChanged(Editable s){}
        });

        loadSaved();
        loadApps();
    }

    void loadSaved() {
        String saved = RootUtils.loadPref("app_modes","");
        String enabled = RootUtils.loadPref("app_enabled","");
        if(!saved.isEmpty()) for(String e:saved.split(",")) { if(e.contains(":")) { String[]p=e.split(":",2); if(p.length==2) appModes.put(p[0],p[1]); } }
        if(!enabled.isEmpty()) for(String e:enabled.split(",")) appEnabled.put(e,true);
    }

    void saveAll() {
        StringBuilder modes=new StringBuilder(), en=new StringBuilder();
        for(Map.Entry<String,String> e:appModes.entrySet()){if(modes.length()>0)modes.append(",");modes.append(e.getKey()).append(":").append(e.getValue());}
        for(Map.Entry<String,Boolean> e:appEnabled.entrySet()){if(e.getValue()){if(en.length()>0)en.append(",");en.append(e.getKey());}}
        RootUtils.savePref("app_modes",modes.toString());
        RootUtils.savePref("app_enabled",en.toString());
    }

    void loadApps() {
        exec.execute(() -> {
            PackageManager pm = ctx.getPackageManager();
            List<ApplicationInfo> list = pm.getInstalledApplications(0);
            allApps.clear();
            // Priority apps first
            String[] priority = {"net.jahez.fleets","io.suqi8.saned","com.google.android.apps.maps"};
            Set<String> pSet = new HashSet<>(Arrays.asList(priority));
            List<AppItem> pApps = new ArrayList<>(), others = new ArrayList<>();
            for(ApplicationInfo a:list) {
                if((a.flags&ApplicationInfo.FLAG_SYSTEM)!=0 && !pSet.contains(a.packageName)) continue;
                AppItem item = new AppItem(pm.getApplicationLabel(a).toString(), a.packageName);
                if(pSet.contains(a.packageName)) pApps.add(item); else others.add(item);
            }
            others.sort((a,b)->a.label.compareToIgnoreCase(b.label));
            allApps.addAll(pApps); allApps.addAll(others);
            handler.post(()->render(allApps));
        });
    }

    void render(List<AppItem> apps) {
        container.removeAllViews();
        for(AppItem app : apps) {
            // Outer card
            LinearLayout card = new LinearLayout(ctx);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.parseColor("#080E18"));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            clp.setMargins(0,0,0,3); card.setLayoutParams(clp);

            // Row
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(14,12,14,12);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            boolean isEnabled = Boolean.TRUE.equals(appEnabled.get(app.pkg));

            // Enable indicator
            TextView tvDot = new TextView(ctx);
            tvDot.setText(isEnabled ? "●" : "○");
            tvDot.setTextColor(Color.parseColor(isEnabled?"#00FF88":"#405060"));
            tvDot.setTextSize(14); tvDot.setPadding(0,0,10,0);

            // App info
            LinearLayout info = new LinearLayout(ctx);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
            TextView tvName = new TextView(ctx);
            tvName.setText(app.label);
            tvName.setTextColor(Color.parseColor(isEnabled?"#D0E8FF":"#6080A0"));
            tvName.setTextSize(13); tvName.setTypeface(null,android.graphics.Typeface.BOLD);
            TextView tvPkg = new TextView(ctx);
            tvPkg.setText(app.pkg); tvPkg.setTextColor(Color.parseColor("#405060")); tvPkg.setTextSize(10);
            info.addView(tvName); info.addView(tvPkg);

            // Mode badge
            TextView tvModeBadge = new TextView(ctx);
            String mode = appModes.getOrDefault(app.pkg,"balanced");
            tvModeBadge.setText(isEnabled ? mode.toUpperCase() : "");
            tvModeBadge.setTextColor(Color.parseColor("performance".equals(mode)?"#00B4FF":"gaming".equals(mode)?"#FF8C00":"#00FF88"));
            tvModeBadge.setTextSize(11); tvModeBadge.setTypeface(null,android.graphics.Typeface.BOLD);

            row.addView(tvDot); row.addView(info); row.addView(tvModeBadge);
            card.addView(row);

            // Expand panel (hidden by default)
            LinearLayout expandPanel = new LinearLayout(ctx);
            expandPanel.setOrientation(LinearLayout.VERTICAL);
            expandPanel.setBackgroundColor(Color.parseColor("#0A1628"));
            expandPanel.setPadding(14,8,14,12);
            expandPanel.setVisibility(View.GONE);

            // Enable switch
            LinearLayout swRow = new LinearLayout(ctx);
            swRow.setOrientation(LinearLayout.HORIZONTAL);
            swRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            swRow.setPadding(0,4,0,8);
            TextView swLabel = new TextView(ctx);
            swLabel.setText("Enable for this app");
            swLabel.setTextColor(Color.parseColor("#D0E8FF")); swLabel.setTextSize(13);
            swLabel.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
            Switch sw = new Switch(ctx);
            sw.setChecked(isEnabled);
            swRow.addView(swLabel); swRow.addView(sw);
            expandPanel.addView(swRow);

            // Mode options
            TextView modeLabel = new TextView(ctx);
            modeLabel.setText("Select Mode:"); modeLabel.setTextColor(Color.parseColor("#6080A0"));
            modeLabel.setTextSize(11); modeLabel.setPadding(0,4,0,6);
            expandPanel.addView(modeLabel);

            LinearLayout modeRow = new LinearLayout(ctx);
            modeRow.setOrientation(LinearLayout.HORIZONTAL);
            Button btnBal = mkModeBtn("BALANCED","#00FF88");
            Button btnPerf = mkModeBtn("PERFORMANCE","#00B4FF");
            Button btnGame = mkModeBtn("GAMING","#FF8C00");
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1);
            mlp.setMargins(0,0,4,0);
            modeRow.addView(btnBal,mlp); modeRow.addView(btnPerf,mlp);
            modeRow.addView(btnGame,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
            expandPanel.addView(modeRow);

            // Update button highlights based on current mode
            updateModeButtons(btnBal,btnPerf,btnGame,mode);

            // Mode button listeners
            btnBal.setOnClickListener(x->{appModes.put(app.pkg,"balanced");tvModeBadge.setText("BALANCED");tvModeBadge.setTextColor(Color.parseColor("#00FF88"));updateModeButtons(btnBal,btnPerf,btnGame,"balanced");saveAll();});
            btnPerf.setOnClickListener(x->{appModes.put(app.pkg,"performance");tvModeBadge.setText("PERFORMANCE");tvModeBadge.setTextColor(Color.parseColor("#00B4FF"));updateModeButtons(btnBal,btnPerf,btnGame,"performance");saveAll();});
            btnGame.setOnClickListener(x->{appModes.put(app.pkg,"gaming");tvModeBadge.setText("GAMING");tvModeBadge.setTextColor(Color.parseColor("#FF8C00"));updateModeButtons(btnBal,btnPerf,btnGame,"gaming");saveAll();});

            sw.setOnCheckedChangeListener((btn,checked)->{
                appEnabled.put(app.pkg,checked);
                tvDot.setText(checked?"●":"○"); tvDot.setTextColor(Color.parseColor(checked?"#00FF88":"#405060"));
                tvName.setTextColor(Color.parseColor(checked?"#D0E8FF":"#6080A0"));
                tvModeBadge.setText(checked?appModes.getOrDefault(app.pkg,"balanced").toUpperCase():"");
                saveAll();
            });

            card.addView(expandPanel);

            // Click to toggle expand
            row.setOnClickListener(x -> {
                if(expandPanel.getVisibility()==View.GONE) expandPanel.setVisibility(View.VISIBLE);
                else expandPanel.setVisibility(View.GONE);
            });

            container.addView(card);
        }
    }

    Button mkModeBtn(String text, String color) {
        Button b = new Button(ctx);
        b.setText(text); b.setTextSize(10); b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.parseColor("#152035")); b.setPadding(4,6,4,6);
        return b;
    }

    void updateModeButtons(Button bal, Button perf, Button game, String mode) {
        bal.setBackgroundColor(Color.parseColor("balanced".equals(mode)?"#00FF88":"#152035"));
        bal.setTextColor(Color.parseColor("balanced".equals(mode)?"#000000":"#FFFFFF"));
        perf.setBackgroundColor(Color.parseColor("performance".equals(mode)?"#00B4FF":"#152035"));
        perf.setTextColor(Color.parseColor("performance".equals(mode)?"#000000":"#FFFFFF"));
        game.setBackgroundColor(Color.parseColor("gaming".equals(mode)?"#FF8C00":"#152035"));
        game.setTextColor(Color.parseColor("gaming".equals(mode)?"#000000":"#FFFFFF"));
    }

    void filter(String q) {
        if(q.isEmpty()){render(allApps);return;}
        List<AppItem> f=new ArrayList<>();
        for(AppItem a:allApps) if(a.label.toLowerCase().contains(q.toLowerCase())||a.pkg.toLowerCase().contains(q.toLowerCase())) f.add(a);
        render(f);
    }
}
