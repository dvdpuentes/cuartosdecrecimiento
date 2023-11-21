package com.example.arduinoconnector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                // Realiza las acciones que deseas al iniciarse el dispositivo aquí.
                // Puedes iniciar servicios, configuraciones, etc.
            }
        }
    }
}
