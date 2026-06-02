package com.rootmasterbd.saned;

import android.graphics.Color;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class HomeFragment {
    private TextView tvOryon,tvCpu,tvGpu,tvHz,tvFps;
    private TextView tvJahez,tvSaned,tvMaps;
    private TextView tvNetMs,tvJahezMs,tvDl,tvUl;
    private TextView tvWakelock,tvGps,tvKa,tvBg,tvLog,tvMode;
    private Button btnB,btnP,btnG;
    private Handler handler;
    private ExecutorService exec;
    private Runnable updateTask;
    private boolean running = true;

    public HomeFragment(View v, Handler h, ExecutorService e, TextView modeTV) {
        handler=h; exec=e; tvMode=modeTV;
        tvOryon=v.findViewById(R.id.tvOryon); tvCpu=v.findViewById(R.id.tvCpu);
        tvGpu=v.findViewById(R.id.tvGpu); tvHz=v.findViewById(R.id.tvHz);
        tvFps=v.findViewById(R.id.tvFps); tvJahez=v.findViewById(R.id.tvJahez);
        tvSaned=v.findViewById(R.id.tvSaned); tvMaps=v.findViewById(R.id.tvMaps);
        tvNetMs=v.findViewById(R.id.tvNetMs); tvJahezMs=v.findViewById(R.id.tvJahezMs);
        tvDl=v.findViewById(R.id.tvDl); tvUl=v.findViewById(R.id.tvUl);
        tvWakelock=v.findViewById(R.id.tvWakelock); tvGps=v.findViewById(R.id.tvGps);
        tvKa=v.findViewById(R.id.tvKa); tvBg=v.findViewById(R.id.tvBg);
        tvLog=v.findViewById(R.id.tvLog);
        btnB=v.findViewById(R.id.btnBalanced); btnP=v.findViewById(R.id.btnPerformance); btnG=v.findViewById(R.id.btnGaming);

        btnB.setOnClickListener(x -> setMode("idle"));
        btnP.setOnClickListener(x -> setMode("active"));
        btnG.setOnClickListener(x -> setMode("gaming"));

        // Load current mode
        exec.execute(() -> { String m = RootUtils.getMode(); handler.post(() -> updateButtons(m)); });
        startUpdates();
        log("RootmasterBD Saned v1.0");
    }

    void setMode(String m) {
        exec.execute(() -> { RootUtils.setMode(m); log("Mode → "+m.toUpperCase()); handler.post(()->updateButtons(m)); });
    }

    void updateButtons(String m) {
        btnB.setBackgroundColor(Color.parseColor("idle".equals(m)?"#00FF88":"#0A1628"));
        btnB.setTextColor(Color.parseColor("idle".equals(m)?"#000000":"#00FF88"));
        btnP.setBackgroundColor(Color.parseColor("active".equals(m)?"#00B4FF":"#0A1628"));
        btnP.setTextColor(Color.parseColor("active".equals(m)?"#000000":"#00B4FF"));
        btnG.setBackgroundColor(Color.parseColor("gaming".equals(m)?"#FF8C00":"#0A1628"));
        btnG.setTextColor(Color.parseColor("gaming".equals(m)?"#000000":"#FF8C00"));
        tvMode.setText("["+m.toUpperCase()+"]");
        tvMode.setTextColor(Color.parseColor("idle".equals(m)?"#FFD700":"#00FF88"));
    }

    void startUpdates() {
        updateTask = new Runnable() {
            public void run() {
                if(!running) return;
                exec.execute(() -> {
                    Map<String,String> d = RootUtils.readData();
                    // Also check app status directly
                    String jahez = d.getOrDefault("JAHEZ","");
                    String saned = d.getOrDefault("SANED","");
                    if(jahez.isEmpty() || "Inactive".equals(jahez)) {
                        jahez = RootUtils.getAppStatus("net.jahez.fleets");
                    }
                    if(saned.isEmpty() || "Inactive".equals(saned)) {
                        saned = RootUtils.getAppStatus("io.suqi8.saned");
                    }
                    final String fJahez=jahez, fSaned=saned;
                    handler.post(() -> { update(d); setApp(tvJahez,"Jahez",fJahez); setApp(tvSaned,"Saned",fSaned); });
                });
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateTask);
    }

    void update(Map<String,String> d) {
        String o=d.getOrDefault("ORYON","0|0"); String[]op=o.split("\\|");
        int ov=si(op.length>0?op[0]:"0");
        tvOryon.setText("Oryon: "+(op.length>0?op[0]:"0")+"MHz | "+(op.length>1?op[1]:"0")+"MHz");
        tvOryon.setTextColor(Color.parseColor(ov>=4000?"#00FF88":ov>=3000?"#FFD700":"#FF4060"));
        tvCpu.setText("CPU: "+d.getOrDefault("CPU","---"));
        tvGpu.setText("GPU: "+d.getOrDefault("GPU","0")+"%");
        tvHz.setText(d.getOrDefault("HZ","144")+"Hz");
        tvFps.setText("FPS: "+d.getOrDefault("FPS","---"));
        setApp(tvMaps,"Maps",d.getOrDefault("MAPS","Inactive"));
        tvNetMs.setText("Net MS: "+d.getOrDefault("MS_NET",d.getOrDefault("MS","---")));
        String jms=d.getOrDefault("MS_JAHEZ","---");
        tvJahezMs.setText("Jahez MS: "+jms);
        int mv=si(jms.replace("ms","").trim());
        tvJahezMs.setTextColor(Color.parseColor(mv>0&&mv<=20?"#00FF88":mv<=50?"#FFD700":"#FF4060"));
        String net=d.getOrDefault("NET","0|0"); String[]np=net.split("\\|");
        tvDl.setText("↓ "+(np.length>0?np[0]:"0")+" KB/s");
        tvUl.setText("↑ "+(np.length>1?np[1]:"0")+" KB/s");
        setSt(tvWakelock,"Wakelock",d.getOrDefault("WAKELOCK","OFF"),"ON");
        setSt(tvGps,"GPS",d.getOrDefault("GPS","OFF"),"HIGH_ACC");
        setSt(tvKa,"KA",d.getOrDefault("KEEPALIVE","---"),"PROTECTED");
        setSt(tvBg,"BG",d.getOrDefault("BGDATA","---"),"ALLOWED");
    }

    void setApp(TextView tv,String l,String s){tv.setText(l+": "+s);tv.setTextColor(Color.parseColor("Foreground".equals(s)?"#00FF88":"Background".equals(s)?"#FFD700":"#FF4060"));}
    void setSt(TextView tv,String l,String v,String g){tv.setText(l+": "+v);tv.setTextColor(Color.parseColor(v.equals(g)?"#00FF88":"#FF4060"));}
    void log(String msg){handler.post(()->{String cur=tvLog.getText().toString();String[]lines=cur.split("\n");StringBuilder sb=new StringBuilder();int s=lines.length>25?lines.length-25:0;for(int i=s;i<lines.length;i++)sb.append(lines[i]).append("\n");sb.append("["+new SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(new java.util.Date())+"] "+msg);tvLog.setText(sb.toString());});}
    int si(String s){try{return Integer.parseInt(s.trim());}catch(Exception e){return 0;}}
    public void stop(){ running=false; if(updateTask!=null) handler.removeCallbacks(updateTask); }
}
