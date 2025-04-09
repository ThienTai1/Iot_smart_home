package com.phamthientai.ble_prov;

import com.espressif.provisioning.ESPDevice;

/**
 * Singleton class to manage ESP device session across activities
 */
public class DeviceSessionManager {
    private static DeviceSessionManager instance;
    private ESPDevice espDevice;

    private DeviceSessionManager() {}

    public static synchronized DeviceSessionManager getInstance() {
        if (instance == null) {
            instance = new DeviceSessionManager();
        }
        return instance;
    }

    public void setEspDevice(ESPDevice device) {
        this.espDevice = device;
    }

    public ESPDevice getEspDevice() {
        return espDevice;
    }

    public void clearSession() {
        espDevice = null;
    }
}