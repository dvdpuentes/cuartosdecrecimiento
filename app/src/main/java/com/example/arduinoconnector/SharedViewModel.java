package com.example.arduinoconnector;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<List<Sensor>> sensorsDataFirstFragment = new MutableLiveData<>();

    // Datos para SecondFragment
    private final MutableLiveData<List<Sensor>> sensorsDataSecondFragment = new MutableLiveData<>();
    private final MutableLiveData<Integer> sensorPosition = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isBluetoothEnabled = new MutableLiveData<>();
    // Resto de tus propiedades y métodos existentes

    private final MutableLiveData<FirstFragment.ConnectionStatusEnum> isBluetoothConnected = new MutableLiveData<>();

    public LiveData<FirstFragment.ConnectionStatusEnum> getIsBluetoothConnected() {
        return isBluetoothConnected;
    }

    public void setIsBluetoothConnected(FirstFragment.ConnectionStatusEnum isConnected) {
        isBluetoothConnected.setValue(isConnected);
    }

    public void setBluetoothEnabled(boolean enabled) {
        isBluetoothEnabled.setValue(enabled);
    }

    public LiveData<Boolean> isBluetoothEnabled() {
        return isBluetoothEnabled;
    }

    // Métodos para FirstFragment
    public void setSensorsDataFirstFragment(List<Sensor> sensors) {
        sensorsDataFirstFragment.setValue(sensors);
    }

    public LiveData<List<Sensor>> getSensorsDataFirstFragment() {
        return sensorsDataFirstFragment;
    }

    // Métodos para SecondFragment
    public void setSensorsDataSecondFragment(List<Sensor> sensors) {
        sensorsDataSecondFragment.setValue(sensors);
    }

    public LiveData<List<Sensor>> getSensorsDataSecondFragment() {
        return sensorsDataSecondFragment;
    }
    public void setSensorPosition(int position) {
        sensorPosition.setValue(position);
    }

    public LiveData<Integer> getSensorPosition() {
        return sensorPosition;
    }
}

