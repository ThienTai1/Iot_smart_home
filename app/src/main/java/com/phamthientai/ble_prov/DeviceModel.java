package com.phamthientai.ble_prov;

public class DeviceModel {
    private String name;
    private int iconResId;
    private String room;
    private String status = "Unknown" ;
    public DeviceModel(String name, int iconResId, String room, String status) {
        this.name = name;
        this.iconResId = iconResId;
        this.room = room;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }
    public String getRoom() {
        return room;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
