package com.rootmasterbd.saned;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppListActivity extends Activity {

    private LinearLayout appListContainer;
    private EditText etSearch;
    private TextView tvTitle;
    private Button btnSave;
    private ScrollView scrollView;

    private List<AppItem> allApps = new ArrayList<>();
    private Set<String> selectedPackages = new HashSet<>();
    private String targetMode = "performance"; // which mode this app gets
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    static class AppItem {
        String label, packageName, mode;
        boolean selected;
        AppItem(String l, String p, String m, boolean s) {
            label = l; packageName = p; mode = m; selected = s;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applist);

        appListContainer = findViewById(R.id.appListContainer);
        etSearch = findViewById(R.id.etSearch);
        tvTitle = findViewById(R.id.tvAppListTitle);
        btnSave = findViewById(R.id.btnSaveApps);
        scrollView = findViewById(R.id.scrollApps);

        tvTitle.setText("App Mode Assignment");

        loadSavedApps();
        loadApps();

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            public void onTextChanged(CharSequence s, int i, int b, int c) { filterApps(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });

        btnSave.setOnClickListener(v -> saveApps());
    }

    private void loadSavedApps() {
        String saved = RootUtils.loadPref("app_modes", "");
        if (!saved.isEmpty()) {
            for (String entry : saved.split(",")) {
                if (entry.contains(":")) {
                    String[] parts = entry.split(":");
                    if (parts.length >= 2) selectedPackages.add(parts[0]);
                }
            }
        }
    }

    private void loadApps() {
        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            allApps.clear();

            // Protected/default apps at top
            String[] defaultProtected = {
                "net.jahez.fleets", "io.suqi8.saned", "com.google.android.apps.maps"
            };

            Set<String> defaultSet = new HashSet<>(Arrays.asList(defaultProtected));

            // Load saved modes
            String savedModes = RootUtils.loadPref("app_modes", "");
            Map<String, String> savedMap = new HashMap<>();
            if (!savedModes.isEmpty()) {
                for (String entry : savedModes.split(",")) {
                    if (entry.contains(":")) {
                        String[] p = entry.split(":", 2);
                        if (p.length == 2) savedMap.put(p[0], p[1]);
                    }
                }
            }

            for (ApplicationInfo info : apps) {
                if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                String label = pm.getApplicationLabel(info).toString();
                String pkg = info.packageName;
                boolean isDefault = defaultSet.contains(pkg);
                String mode = savedMap.getOrDefault(pkg, isDefault ? "performance" : "balanced");
                boolean selected = savedMap.containsKey(pkg) || isDefault;
                allApps.add(new AppItem(label, pkg, mode, selected || isDefault));
            }

            // Sort: selected first, then alphabetical
            allApps.sort((a, b) -> {
                if (a.selected != b.selected) return a.selected ? -1 : 1;
                return a.label.compareToIgnoreCase(b.label);
            });

            handler.post(() -> renderApps(allApps));
        });
    }

    private void renderApps(List<AppItem> apps) {
        appListContainer.removeAllViews();
        for (AppItem app : apps) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_app, appListContainer, false);

            TextView tvName = row.findViewById(R.id.tvAppName);
            TextView tvPkg = row.findViewById(R.id.tvAppPkg);
            CheckBox cbSelect = row.findViewById(R.id.cbAppSelect);
            Spinner spMode = row.findViewById(R.id.spAppMode);

            tvName.setText(app.label);
            tvPkg.setText(app.packageName);
            cbSelect.setChecked(app.selected);

            ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"balanced", "performance", "gaming"});
            modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spMode.setAdapter(modeAdapter);

            int modeIdx = app.mode.equals("performance") ? 1 : app.mode.equals("gaming") ? 2 : 0;
            spMode.setSelection(modeIdx);
            spMode.setVisibility(app.selected ? View.VISIBLE : View.GONE);

            cbSelect.setOnCheckedChangeListener((btn, checked) -> {
                app.selected = checked;
                spMode.setVisibility(checked ? View.VISIBLE : View.GONE);
            });

            spMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    app.mode = new String[]{"balanced", "performance", "gaming"}[pos];
                }
                public void onNothingSelected(AdapterView<?> p) {}
            });

            appListContainer.addView(row);
        }
    }

    private void filterApps(String query) {
        if (query.isEmpty()) { renderApps(allApps); return; }
        List<AppItem> filtered = new ArrayList<>();
        for (AppItem a : allApps) {
            if (a.label.toLowerCase().contains(query.toLowerCase()) ||
                a.packageName.toLowerCase().contains(query.toLowerCase()))
                filtered.add(a);
        }
        renderApps(filtered);
    }

    private void saveApps() {
        executor.execute(() -> {
            StringBuilder sb = new StringBuilder();
            List<String> perfApps = new ArrayList<>();
            List<String> gamingApps = new ArrayList<>();
            List<String> balancedApps = new ArrayList<>();

            for (AppItem app : allApps) {
                if (!app.selected) continue;
                if (sb.length() > 0) sb.append(",");
                sb.append(app.packageName).append(":").append(app.mode);

                if ("performance".equals(app.mode)) perfApps.add(app.packageName);
                else if ("gaming".equals(app.mode)) gamingApps.add(app.packageName);
                else balancedApps.add(app.packageName);
            }

            RootUtils.savePref("app_modes", sb.toString());

            // Write to module
            String perfList = String.join(" ", perfApps);
            String gamingList = String.join(" ", gamingApps);
            RootUtils.runSu("echo 'PERF_APPS=\"" + perfList + "\"' > /data/local/tmp/bgguard_app_modes");
            RootUtils.runSu("echo 'GAMING_APPS=\"" + gamingList + "\"' >> /data/local/tmp/bgguard_app_modes");

            handler.post(() -> Toast.makeText(this, "Saved! " + (perfApps.size() + gamingApps.size()) + " apps configured", Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
