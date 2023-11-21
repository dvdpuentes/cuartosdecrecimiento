package com.example.arduinoconnector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

public class WifiReceiver extends BroadcastReceiver {
    public OnWifiSignalUpdateListener listener;

    public void setOnWifiSignalUpdateListener(OnWifiSignalUpdateListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Obtiene la intensidad de la señal Wi-Fi
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int rssi = wifiManager.getConnectionInfo().getRssi();

        // Convierte la intensidad de la señal en un nivel (por ejemplo, de -100 a -50)
        int wifiLevel = WifiManager.calculateSignalLevel(rssi, 5); // 5 niveles de intensidad

        // Notifica a la interfaz registrada
        if (listener != null) {
            listener.onWifiSignalUpdate(wifiLevel);
        }
    }

    public interface OnWifiSignalUpdateListener {
        void onWifiSignalUpdate(int wifiLevel);
    }
}

