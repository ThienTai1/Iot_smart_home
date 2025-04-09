package com.phamthientai.ble_prov;

import android.app.Application;
import android.util.Log;

import com.espressif.provisioning.ESPDevice;

public class MyApp extends Application {
    private static MyApp instance;
    private ESPDevice espDevice;

    public static MyApp getInstance() {
        return instance;
    }

    public void setEspDevice(ESPDevice device) {
        Log.d("MyApp", "setEspDevice: " + device.getDeviceName());
        this.espDevice = device;
    }

    public ESPDevice getEspDevice() {
        return espDevice;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d("MyApp", "MyApp.onCreate called");
    }
}

