package br.com.jcducorte084.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean enabled = context.getSharedPreferences("jc_barber", Context.MODE_PRIVATE)
                .getBoolean("monitor_enabled", false);
        if (enabled) BookingWatchService.start(context);
    }
}
