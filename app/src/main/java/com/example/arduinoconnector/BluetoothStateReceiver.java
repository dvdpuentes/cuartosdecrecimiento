package com.example.arduinoconnector;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothStateReceiver extends BroadcastReceiver {

    private SharedViewModel sharedViewModel;

    public BluetoothStateReceiver(SharedViewModel viewModel) {
        sharedViewModel = viewModel;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    // Bluetooth está apagado
                    sharedViewModel.setBluetoothEnabled(false);
                    break;
                case BluetoothAdapter.STATE_ON:
                    // Bluetooth está encendido
                    sharedViewModel.setBluetoothEnabled(true);
                    break;
                // Otros casos según sea necesario
            }
        }
    }
}

