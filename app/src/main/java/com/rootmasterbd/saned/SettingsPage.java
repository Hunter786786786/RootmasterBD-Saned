package com.rootmasterbd.saned;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import java.util.concurrent.ExecutorService;
public class SettingsPage {
    public SettingsPage(Context ctx, View v, ExecutorService exec, Handler handler) {
        Switch swAuto=v.findViewById(R.id.swAutoSwitch),swKill=v.findViewById(R.id.swKillAll);
        EditText etWl=v.findViewById(R.id.etWhitelist);
        Button btnKill=v.findViewById(R.id.btnKillNow),btnSave=v.findViewById(R.id.btnSaveSettings);
        TextView tvSt=v.findViewById(R.id.tvSettingsStatus);
        btnSave.setBackgroundColor(Color.parseColor("#00FF88")); btnSave.setTextColor(Color.BLACK);
        btnKill.setBackgroundColor(Color.parseColor("#FF4060")); btnKill.setTextColor(Color.WHITE);
        swAuto.setChecked("1".equals(RootUtils.loadPref("auto_switch","0")));
        swKill.setChecked("1".equals(RootUtils.loadPref("kill_all","0")));
        etWl.setText(RootUtils.loadPref("whitelist","net.jahez.fleets io.suqi8.saned com.google.android.apps.maps com.termux"));
        btnSave.setOnClickListener(x->{
            RootUtils.savePref("auto_switch",swAuto.isChecked()?"1":"0");
            RootUtils.savePref("kill_all",swKill.isChecked()?"1":"0");
            RootUtils.savePref("whitelist",etWl.getText().toString().trim());
            tvSt.setText("✓ Saved!"); tvSt.setTextColor(Color.parseColor("#00FF88"));
        });
        btnKill.setOnClickListener(x->{
            tvSt.setText("Killing..."); tvSt.setTextColor(Color.parseColor("#FFD700"));
            exec.execute(()->{
                String wl=etWl.getText().toString().trim().replace(" ","|");
                RootUtils.su("for pid in $(ls /proc/|grep -E '^[0-9]+$');do pkg=$(cat /proc/$pid/cmdline 2>/dev/null|tr -d '\\0'|cut -d: -f1);echo \"$pkg\"|grep -q '\\.'||continue;echo \"$pkg\"|grep -qE '"+wl+"|system|zygote|android|com.google.android.gms'&&continue;kill -9 $pid 2>/dev/null;done");
                handler.post(()->{tvSt.setText("✓ Done!");tvSt.setTextColor(Color.parseColor("#00FF88"));});
            });
        });
    }
}
