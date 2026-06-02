package com.rootmasterbd.saned;
import android.graphics.Color;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import java.util.concurrent.ExecutorService;
public class TweaksPage {
    public TweaksPage(View v, ExecutorService exec, Handler handler) {
        Switch swCpu=v.findViewById(R.id.swCpuMax),swGpu=v.findViewById(R.id.swGpuMax);
        Switch sw144=v.findViewById(R.id.sw144hz),swT=v.findViewById(R.id.swThermal);
        Switch swNet=v.findViewById(R.id.swNetwork),swDns=v.findViewById(R.id.swDns);
        Switch swWl=v.findViewById(R.id.swWakelock),swGps=v.findViewById(R.id.swGps);
        TextView tvSt=v.findViewById(R.id.tvTweakStatus);
        Button btnApply=v.findViewById(R.id.btnApplyTweaks);
        btnApply.setBackgroundColor(Color.parseColor("#FF8C00"));
        btnApply.setTextColor(Color.BLACK);
        swCpu.setChecked("1".equals(RootUtils.loadPref("tw_cpu","1")));
        swGpu.setChecked("1".equals(RootUtils.loadPref("tw_gpu","1")));
        sw144.setChecked("1".equals(RootUtils.loadPref("tw_144","1")));
        swT.setChecked("1".equals(RootUtils.loadPref("tw_thermal","1")));
        swNet.setChecked("1".equals(RootUtils.loadPref("tw_net","1")));
        swDns.setChecked("1".equals(RootUtils.loadPref("tw_dns","1")));
        swWl.setChecked("1".equals(RootUtils.loadPref("tw_wl","1")));
        swGps.setChecked("1".equals(RootUtils.loadPref("tw_gps","1")));
        btnApply.setOnClickListener(x->{
            tvSt.setText("Applying..."); tvSt.setTextColor(Color.parseColor("#FFD700"));
            exec.execute(()->{
                RootUtils.savePref("tw_cpu",swCpu.isChecked()?"1":"0");
                RootUtils.savePref("tw_gpu",swGpu.isChecked()?"1":"0");
                RootUtils.savePref("tw_144",sw144.isChecked()?"1":"0");
                RootUtils.savePref("tw_thermal",swT.isChecked()?"1":"0");
                RootUtils.savePref("tw_net",swNet.isChecked()?"1":"0");
                RootUtils.savePref("tw_dns",swDns.isChecked()?"1":"0");
                RootUtils.savePref("tw_wl",swWl.isChecked()?"1":"0");
                RootUtils.savePref("tw_gps",swGps.isChecked()?"1":"0");
                if(swNet.isChecked()) RootUtils.su("sh "+RootUtils.MODDIR+"/network_opt.sh &");
                if(swGps.isChecked()) RootUtils.su("sh "+RootUtils.MODDIR+"/gps_lock.sh &");
                if(swWl.isChecked()) RootUtils.su("sh "+RootUtils.MODDIR+"/wakelock_guard.sh &");
                if(sw144.isChecked()){RootUtils.su("setprop persist.sys.display.refresh_rate 144");RootUtils.su("setprop persist.vendor.display.refresh_rate 144");}
                if(swDns.isChecked()){RootUtils.su("setprop net.dns1 1.1.1.1");RootUtils.su("setprop net.dns2 1.0.0.1");}
                if(swT.isChecked()) RootUtils.su("for tz in /sys/devices/virtual/thermal/thermal_zone*/trip_point_0_temp; do echo 95000 > $tz 2>/dev/null; done");
                handler.post(()->{tvSt.setText("✓ Applied!");tvSt.setTextColor(Color.parseColor("#00FF88"));});
            });
        });
    }
}
