// Cập nhật DeviceAdapter.java để:
// ✅ Nhấn icon -> bật/tắt MQTT
// ✅ Nhấn menu 3 chấm -> cấu hình IP hoặc xoá thiết bị

package com.phamthientai.ble_prov;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.phamthientai.ble_prov.util.MqttManager;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private final List<DeviceModel> deviceList;
    private final Context context;

    public DeviceAdapter(Context context, List<DeviceModel> deviceList) {
        this.context = context;
        this.deviceList = deviceList;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        DeviceModel device = deviceList.get(position);
        holder.tvDeviceName.setText(device.getName());

        // Trạng thái LED
        boolean isOn = device.isOn();
        holder.imgDeviceIcon.setImageResource(isOn ? R.drawable.ic_light_on : R.drawable.ic_light_off);

        holder.imgDeviceIcon.setOnClickListener(v -> {
            boolean newState = !device.isOn();
            device.setOn(newState);
            notifyItemChanged(position);

            // Gửi lệnh MQTT
            SharedPreferences prefs = context.getSharedPreferences("mqtt_config_" + device.getName(), Context.MODE_PRIVATE);
            String ip = prefs.getString("ip", "");
            String port = prefs.getString("port", "");

            if (!ip.isEmpty() && !port.isEmpty()) {
                MqttManager.publishCommand(context, ip, port, newState ? "ON" : "OFF");
            } else {
                Toast.makeText(context, "Thiết bị chưa cấu hình IP/Port MQTT", Toast.LENGTH_SHORT).show();
            }
        });

        holder.imgMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.imgMenu);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.device_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_config_mqtt) {
                    showMqttConfigDialog(device);
                    return true;
                } else if (item.getItemId() == R.id.menu_delete_device) {
                    showDeleteDeviceDialog(position);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void showMqttConfigDialog(DeviceModel device) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_mqtt_config, null);
        EditText etIp = dialogView.findViewById(R.id.etDialogIp);
        EditText etPort = dialogView.findViewById(R.id.etDialogPort);

        SharedPreferences prefs = context.getSharedPreferences("mqtt_config_" + device.getName(), Context.MODE_PRIVATE);
        etIp.setText(prefs.getString("ip", ""));
        etPort.setText(prefs.getString("port", ""));

        new AlertDialog.Builder(context)
                .setTitle("Cấu hình MQTT cho " + device.getName())
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String ip = etIp.getText().toString().trim();
                    String port = etPort.getText().toString().trim();

                    prefs.edit().putString("ip", ip).putString("port", port).apply();
                    Toast.makeText(context, "Đã lưu MQTT", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void showDeleteDeviceDialog(int position) {
        new AlertDialog.Builder(context)
                .setTitle("Xoá thiết bị")
                .setMessage("Bạn có chắc muốn xoá thiết bị này?")
                .setPositiveButton("Xoá", (dialog, which) -> {
                    deviceList.remove(position);
                    notifyItemRemoved(position);
                    saveDeviceList();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void saveDeviceList() {
        SharedPreferences prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(deviceList);
        editor.putString("device_list", json);
        editor.apply();
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        ImageView imgDeviceIcon, imgMenu;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            imgDeviceIcon = itemView.findViewById(R.id.imgDeviceIcon);
            imgMenu = itemView.findViewById(R.id.imgDeviceMenu);
        }
    }
}
