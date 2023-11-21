package com.example.arduinoconnector;

import android.os.Build;

import java.time.Instant;
import java.util.Date;

public class Sensor  {
    int address;
    float temperature;
    float humidity;
    String valveState;
    String mode;
    int identifier;
    float temperatureSet;

    Date fecha;
    int cntWait = 0;
    String titulo;
    int cuartoNum;

    // Constructor
    public Sensor(int address, float temperature, float humidity, String valveState, String mode, int identifier, float temperatureSet) {
        this.address = address;
        this.temperature = temperature;
        this.humidity = humidity;
        this.valveState = valveState;
        this.mode = mode;
        this.identifier = identifier;
        this.temperatureSet = temperatureSet;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.fecha = Date.from(Instant.now());
        }
    }
}

