package com.example.arduinoconnector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

public class BatteryReceiver extends BroadcastReceiver {
    public OnBatteryUpdateListener listener;

    public void setOnBatteryUpdateListener(OnBatteryUpdateListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int percentage = (int) ((level / (float) scale) * 100);

        // Verifica si el dispositivo está enchufado y cargando
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

        // Notifica a la interfaz registrada
        if (listener != null) {
            listener.onBatteryUpdate(percentage, isCharging);
        }
    }


    public interface OnBatteryUpdateListener {
        void onBatteryUpdate(int percentage, boolean isCharging);
    }
}



