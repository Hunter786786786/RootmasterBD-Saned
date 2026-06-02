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
    private List<ApplicationInfo> allApps = new ArrayList<>();
    private ExecutorService exec;
    private Handler handler;
    private Context ctx;

    public AppsPage(Context c, ExecutorService e, Handler h) {
        super(c); ctx=c; exec=e; handler=h;
        setBackgroundColor(Color.parseColor("#050A0F"));

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12,12,12,12);

        TextView title = new TextView(c);
        title.setText("APP MODE ASSIGNMENT");
        title.setTextColor(Color.parseColor("#00B4FF"));
        title.setTextSize(14); title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0,0,0,8);
        root.addView(title);

        EditText search = new EditText(c);
        search.setHint("Search apps...");
        search.setHintTextColor(Color.parseColor("#405060"));
        search.setTextColor(Color.parseColor("#D0E8FF"));
        search.setBackgroundColor(Color.parseColor("#080E18"));
        search.setPadding(12,8,12,8);
        root.addView(search);

        container = new LinearLayout(c);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0,8,0,0);
        root.addView(container);

        addView(root);

        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int i,int c,int a){}
            public void onTextChanged(CharSequence s,int i,int b,int c){filter(s.toString());}
            public void afterTextChanged(Editable s){}
        });

        loadApps();
    }

    private void loadApps() {
        exec.execute(() -> {
            PackageManager pm = ctx.getPackageManager();
            List<ApplicationInfo> list = pm.getInstalledApplications(0);
            allApps.clear();
            String[] priority = {"net.jahez.fleets","io.suqi8.saned","com.google.android.apps.maps"};
            Set<String> pSet = new HashSet<>(Arrays.asList(priority));
            for(ApplicationInfo a : list) {
                if((a.flags & ApplicationInfo.FLAG_SYSTEM)==0 || pSet.contains(a.packageName))
                    allApps.add(a);
            }
            allApps.sort((a,b)->pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString()));
            handler.post(()->render(allApps));
        });
    }

    private void render(List<ApplicationInfo> apps) {
        container.removeAllViews();
        PackageManager pm = ctx.getPackageManager();
        String savedModes = RootUtils.loadPref("app_modes","");
        Map<String,String> saved = new HashMap<>();
        if(!savedModes.isEmpty()) for(String e:savedModes.split(",")) { if(e.contains(":")) { String[]p=e.split(":",2); if(p.length==2) saved.put(p[0],p[1]); } }

        for(ApplicationInfo app : apps) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBackgroundColor(Color.parseColor("#080E18"));
            row.setPadding(12,10,12,10);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,0,0,2); row.setLayoutParams(lp);

            LinearLayout info = new LinearLayout(ctx);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));

            TextView tvName = new TextView(ctx);
            tvName.setText(pm.getApplicationLabel(app).toString());
            tvName.setTextColor(Color.parseColor("#D0E8FF")); tvName.setTextSize(13);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvPkg = new TextView(ctx);
            tvPkg.setText(app.packageName);
            tvPkg.setTextColor(Color.parseColor("#405060")); tvPkg.setTextSize(10);

            info.addView(tvName); info.addView(tvPkg);
            row.addView(info);

            Spinner sp = new Spinner(ctx);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, new String[]{"balanced","performance","gaming"});
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sp.setAdapter(adapter);
            String mode = saved.getOrDefault(app.packageName,"balanced");
            sp.setSelection(mode.equals("performance")?1:mode.equals("gaming")?2:0);
            sp.setLayoutParams(new LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT));
            final String pkg = app.packageName;
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                    saved.put(pkg,new String[]{"balanced","performance","gaming"}[pos]);
                    StringBuilder sb=new StringBuilder();
                    for(Map.Entry<String,String> e:saved.entrySet()){if(sb.length()>0)sb.append(",");sb.append(e.getKey()).append(":").append(e.getValue());}
                    RootUtils.savePref("app_modes",sb.toString());
                }
                public void onNothingSelected(AdapterView<?> p){}
            });
            row.addView(sp);
            container.addView(row);
        }
    }

    private void filter(String q) {
        if(q.isEmpty()){render(allApps);return;}
        PackageManager pm = ctx.getPackageManager();
        List<ApplicationInfo> f = new ArrayList<>();
        for(ApplicationInfo a:allApps) if(pm.getApplicationLabel(a).toString().toLowerCase().contains(q.toLowerCase())||a.packageName.toLowerCase().contains(q.toLowerCase())) f.add(a);
        render(f);
    }
}
