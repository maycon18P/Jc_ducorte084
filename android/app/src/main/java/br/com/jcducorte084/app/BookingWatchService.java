package br.com.jcducorte084.app;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.os.IBinder;
import android.util.Base64;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class BookingWatchService extends Service {
    private static final String URL_BASE = "https://pkbqmkszrmukucktlwje.supabase.co";
    private static final String KEY_B64 = "c2JfcHVibGlzaGFibGVfVDRGeHJVMFIyWFBlTWRBdWstUExYQV96QXMyRU13LQ==";
    private static final String CH_MONITOR = "jc_monitor";
    private static final String CH_BOOKINGS = "jc_bookings";
    private ScheduledExecutorService executor;

    public static void start(Context c) {
        Intent i = new Intent(c, BookingWatchService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }
    @Override public void onCreate() {
        super.onCreate(); createChannels(); startForeground(10, monitorNotification());
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(this::pollSafe, 5, 20, TimeUnit.SECONDS);
    }
    @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
    private void createChannels(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationManager nm=getSystemService(NotificationManager.class);
            NotificationChannel m=new NotificationChannel(CH_MONITOR,"Monitor de agendamentos",NotificationManager.IMPORTANCE_LOW);
            m.setSound(null,null); nm.createNotificationChannel(m);
            NotificationChannel b=new NotificationChannel(CH_BOOKINGS,"Novos agendamentos",NotificationManager.IMPORTANCE_HIGH);
            b.enableVibration(true); nm.createNotificationChannel(b);
        }
    }
    private Notification monitorNotification(){
        PendingIntent pi=PendingIntent.getActivity(this,10,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CH_MONITOR):new Notification.Builder(this);
        return b.setSmallIcon(R.drawable.ic_notification).setContentTitle("JC DUCORTE084").setContentText("Monitor de agendamentos ativo").setContentIntent(pi).setOngoing(true).build();
    }
    private void pollSafe(){try{poll();}catch(Throwable ignored){}}
    private void poll() throws Exception{
        String key=new String(Base64.decode(KEY_B64,Base64.DEFAULT),StandardCharsets.UTF_8);
        HttpURLConnection c=(HttpURLConnection)new URL(URL_BASE+"/rest/v1/rpc/get_latest_booking_marker").openConnection();
        c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setRequestMethod("POST");c.setDoOutput(true);
        c.setRequestProperty("apikey",key);c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json");
        try(OutputStream os=c.getOutputStream()){os.write("{}".getBytes(StandardCharsets.UTF_8));}
        int code=c.getResponseCode();if(code<200||code>=300){c.disconnect();return;}
        String response;
        try(BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8))){StringBuilder sb=new StringBuilder();String l;while((l=br.readLine())!=null)sb.append(l);response=sb.toString().trim();}finally{c.disconnect();}
        String marker=response;
        if(marker.isEmpty()||marker.equals("null"))marker="__none__";
        else if(marker.length()>=2&&marker.startsWith(""")&&marker.endsWith("""))marker=marker.substring(1,marker.length()-1);
        SharedPreferences p=getSharedPreferences("jc_barber",MODE_PRIVATE);
        boolean init=p.getBoolean("marker_initialized",false);String old=p.getString("latest_marker","__none__");
        if(!init){p.edit().putString("latest_marker",marker).putBoolean("marker_initialized",true).apply();return;}
        if(!old.equals(marker)){p.edit().putString("latest_marker",marker).apply();if(!"__none__".equals(marker))notifyNew();}
    }
    private void notifyNew(){
        Intent open=new Intent(this,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,11,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CH_BOOKINGS):new Notification.Builder(this);
        Notification n=b.setSmallIcon(R.drawable.ic_notification).setContentTitle("Novo agendamento recebido").setContentText("Abra o painel para confirmar.").setContentIntent(pi).setAutoCancel(true).setDefaults(Notification.DEFAULT_ALL).build();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify((int)(System.currentTimeMillis()&0x7fffffff),n);
    }
    @Override public void onDestroy(){if(executor!=null)executor.shutdownNow();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
