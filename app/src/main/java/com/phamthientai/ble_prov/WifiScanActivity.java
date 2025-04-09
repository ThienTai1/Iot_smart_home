package com.phamthientai.ble_prov;

//import static com.phamthientai.ble_prov.ProvisionDevice.REQUEST_CAMERA_PERMISSION;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.espressif.provisioning.ESPConstants;
import com.espressif.provisioning.ESPDevice;
import com.espressif.provisioning.ESPProvisionManager;
import com.espressif.provisioning.WiFiAccessPoint;
import com.espressif.provisioning.listeners.BleScanListener;
import com.espressif.provisioning.listeners.ProvisionListener;
import com.espressif.provisioning.listeners.ResponseListener;
import com.espressif.provisioning.listeners.WiFiScanListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WifiScanActivity extends AppCompatActivity {

    private static final String TAG = "WifiScanActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_LOCATION_PERMISSION = 102;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 103;

    private ESPDevice espDevice;
    private ESPProvisionManager provisionManager;
    private EditText etDeviceName, etPop, etSSID;
    private TextView tvDeviceName, tvStatus;
    private EditText etPassword, etManualSSID;
    private ListView listWifiNetworks;
    private Button btnScanNetworks, btnProvision;
    private ImageButton btnTogglePassword;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private boolean deviceFound = false;
    private ArrayList<String> wifiNetworks = new ArrayList<>();
    private ArrayAdapter<String> wifiAdapter;
    private String selectedSSID = "";
    private boolean deviceProvisionedSuccessfully = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_scan);

        initViews();
        setupListeners();

        // Initialize Provision Manager
        provisionManager = ESPProvisionManager.getInstance(getApplicationContext());

        // Retrieve device from DeviceSessionManager
        espDevice = DeviceSessionManager.getInstance().getEspDevice();

        if (espDevice == null) {
            // If no device found, go back to QR scan activity
            Toast.makeText(this, "Device information not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get additional info from intent
        Intent intent = getIntent();
        String deviceName = intent.getStringExtra("DEVICE_NAME");
        int transportTypeOrdinal = intent.getIntExtra("TRANSPORT_TYPE", 0);
        String pop = intent.getStringExtra("POP");
        showDeviceConnectDialog();
        // Display device name
        tvDeviceName.setText("Configuring device: " + deviceName);

        // Check if we need to connect to the device based on transport type
        ESPConstants.TransportType transportType = ESPConstants.TransportType.values()[transportTypeOrdinal];

        if (transportType == ESPConstants.TransportType.TRANSPORT_BLE) {
            // For BLE devices, we need to connect
//            connectToBleDevice();
        } else if (transportType == ESPConstants.TransportType.TRANSPORT_SOFTAP) {
            // For SoftAP devices, we need to connect
            connectToSoftApDevice();
        } else {
            // Already connected or unknown transport type
            updateStatus("Ready to scan WiFi networks");
        }
    }

    // In the initViews() method, update the etManualSSID initialization:
    private void initViews() {
        tvDeviceName = findViewById(R.id.tv_device_name);
        etPassword = findViewById(R.id.et_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        etManualSSID = findViewById(R.id.etManualSSID);
        listWifiNetworks = findViewById(R.id.list_wifi_networks);
        btnScanNetworks = findViewById(R.id.btn_scan_networks);
        btnProvision = findViewById(R.id.btn_provision);
        btnBack = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progress_bar);
        tvStatus = findViewById(R.id.tv_status);
        etPop = findViewById(R.id.et_pop);
        etSSID = findViewById(R.id.et_ssid);



        wifiAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiNetworks);
        listWifiNetworks.setAdapter(wifiAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            finish(); // Go back to previous activity
        });

        btnScanNetworks.setOnClickListener(v -> {
            scanWifiNetworks(); // Add this to trigger WiFi scan when button is clicked
        });

        listWifiNetworks.setOnItemClickListener((parent, view, position, id) -> {
            // Highlight the selected item
            listWifiNetworks.setItemChecked(position, true);

            // Get the selected SSID
            selectedSSID = wifiNetworks.get(position);
            etManualSSID.setText(selectedSSID);
        });

        btnProvision.setOnClickListener(v -> {
            String ssid = etManualSSID.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (ssid.isEmpty()) {
                Toast.makeText(this, "Please enter or select a WiFi SSID", Toast.LENGTH_SHORT).show();
                return;
            }

            provisionDevice(ssid, password);
        });

        btnTogglePassword.setOnClickListener(v -> {
            if (etPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
            }
            etPassword.setSelection(etPassword.getText().length());
        });
    }

    //Shows
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void showAlertDialog(String title, String message,
                                 android.content.DialogInterface.OnClickListener positiveListener,
                                 android.content.DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", positiveListener)
                .setNegativeButton("No", negativeListener)
                .show();
    }
    private void showBleDeviceListDialog() {
        // In a real app, you would show a list of found BLE devices
        // For simplicity, we just show a message
        showToast("This is where you would show a list of BLE devices");
    }
    //==============

    private void showDeviceConnectDialog() {
        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Searching for BLE devices...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        provisionManager.searchBleEspDevices("PROV_", new BleScanListener() {
            @Override
            public void scanStartFailed() {
                showToast("BLE off. Let turn on bluetooth");
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onPeripheralFound(BluetoothDevice device, ScanResult scanResult) {
                runOnUiThread(() -> {
                    if (ActivityCompat.checkSelfPermission(WifiScanActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    if (device.getName() != null && device.getName().equals(espDevice.getDeviceName())) {
                        deviceFound = true; // Đánh dấu đã tìm thấy
                        progressBar.setVisibility(View.GONE);
                        List<ParcelUuid> uuids = scanResult.getScanRecord().getServiceUuids();
                        if (uuids != null && !uuids.isEmpty()) {
                            String serviceUuid = uuids.get(0).toString();
                            connectToBleDevice(device, serviceUuid);
                        } else {
                            Log.e("BLE_SCAN", "Không tìm thấy service UUID trong thiết bị BLE.");
                        }
                        Log.d("BLE_SCAN", "Tìm thấy thiết bị: " + device.getName() + ", MAC: " + device.getAddress());
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("BLE scan failed: " + e.getMessage());
                });
            }

            @Override
            public void scanCompleted() {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (!deviceFound) {
                        showAlertDialog("Device not found", "Would you like to manually select a device?",
                                (dialog, which) -> showBleDeviceListDialog(),
                                (dialog, which) -> dialog.dismiss());
                    }
                });
            }
        });
    }


    private void connectToBleDevice(BluetoothDevice device, String serviceUuid) {
        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Connecting to BLE device...");

        try {
            espDevice.connectBLEDevice(device, serviceUuid);

            // ✅ Lấy POP từ Intent thay vì EditText
            String pop = getIntent().getStringExtra("POP");
            if (pop != null && !pop.isEmpty()) {
                espDevice.setProofOfPossession(pop);
            }

            updateStatus("Connected to device: " + espDevice.getDeviceName());

            progressBar.setVisibility(View.GONE);

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            updateStatus("Failed to connect: " + e.getMessage());
        }
    }


    private void connectToSoftApDevice() {
        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Connecting to SoftAP device...");

        try {
            espDevice.connectWiFiDevice();
            String pop = etPop.getText().toString();
            if (!pop.isEmpty()) {
                espDevice.setProofOfPossession(pop);
            }
            // Note: In a real app, you would register for connection events
            // and handle success/failure accordingly
            updateStatus("Connected to device: " + espDevice.getDeviceName());
            progressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            updateStatus("Failed to connect: " + e.getMessage());
        }
    }


    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_CAMERA_PERMISSION);
        }
    }




    private void scanWifiNetworks() {
        if (espDevice == null) {
            Toast.makeText(this, "Device not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Scanning WiFi networks...");
        wifiNetworks.clear();
        wifiAdapter.notifyDataSetChanged();

        try {
            espDevice.scanNetworks(new WiFiScanListener() {
                @Override
                public void onWifiListReceived(ArrayList<WiFiAccessPoint> wifiList) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;

                        progressBar.setVisibility(View.GONE);
                        wifiNetworks.clear();

                        if (wifiList == null) {
                            updateStatus("No WiFi networks found");
                            showManualWifiDialog();
                            return;
                        }

                        for (WiFiAccessPoint ap : wifiList) {
                            // Defensive null checking
                            if (ap == null) continue;

                            String ssid = ap.getWifiName();
                            // Debug information for each AP
                            Log.d(TAG, "Received AP: " + (ssid == null ? "null" : "\"" + ssid + "\""));

                            if (ssid != null && !ssid.trim().isEmpty()) {
                                wifiNetworks.add(ssid);
                            }
                        }

                        Log.d(TAG, "Total filtered SSIDs: " + wifiNetworks.size());

                        if (wifiNetworks.isEmpty()) {
                            updateStatus("No WiFi networks found");
                            showManualWifiDialog();
                        } else {
                            wifiAdapter.notifyDataSetChanged();
                            updateStatus("Found " + wifiNetworks.size() + " WiFi networks");
                        }
                    });
                }

                @Override
                public void onWiFiScanFailed(Exception e) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        progressBar.setVisibility(View.GONE);
                        String errorMsg = e != null ? e.getMessage() : "Unknown error";
                        updateStatus("WiFi scan failed: " + errorMsg);

                        // Show dialog to enter WiFi manually
                        showManualWifiDialog();
                    });
                }
            });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            String errorMsg = e != null ? e.getMessage() : "Unknown error";
            updateStatus("Error during WiFi scan: " + errorMsg);
            showManualWifiDialog(); // Always show manual dialog on exception
        }
    }

    private void showManualWifiDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter WiFi Network Manually");

        View view = getLayoutInflater().inflate(R.layout.dialog_manual_wifi, null);
        builder.setView(view);

        final EditText etDialogManualSSID = view.findViewById(R.id.etManualSSID);
        final Spinner spinnerSecurity = view.findViewById(R.id.spinnerSecurity);

        // Setup security spinner
        ArrayAdapter<String> securityAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"WPA/WPA2", "WEP", "Open (No Security)"}
        );
        spinnerSecurity.setAdapter(securityAdapter);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String ssid = etDialogManualSSID.getText().toString().trim();
            if (!ssid.isEmpty()) {
                this.etManualSSID.setText(ssid);
                selectedSSID = ssid;
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    private void provisionDevice(String ssid, String password) {
        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Provisioning device...");

        espDevice.provision(ssid, password, new ProvisionListener() {
            @Override
            public void createSessionFailed(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("Failed to create session: " + e.getMessage());
                });
            }

            @Override
            public void wifiConfigSent() {
                runOnUiThread(() -> {
                    updateStatus("WiFi configuration sent to device");
                });
            }

            @Override
            public void wifiConfigFailed(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("WiFi configuration failed: " + e.getMessage());
                });
            }

            @Override
            public void wifiConfigApplied() {
                runOnUiThread(() -> {
                    updateStatus("WiFi configuration applied");
                });
            }

            @Override
            public void wifiConfigApplyFailed(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("Failed to apply WiFi configuration: " + e.getMessage());
                });
            }

            @Override
            public void provisioningFailedFromDevice(ESPConstants.ProvisionFailureReason failureReason) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("Provisioning failed: " + failureReason);
                });
            }

            @Override
            public void deviceProvisioningSuccess() {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("Device provisioned successfully!");
                    deviceProvisionedSuccessfully = true;

                    // Show dialog to add device to the app
                    showAddDeviceDialog();
                });
            }

            @Override
            public void onProvisioningFailed(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("Provisioning failed: " + e.getMessage());
                });
            }
        });
    }

    private void showAddDeviceDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_device, null);
        EditText etDeviceName = dialogView.findViewById(R.id.etDeviceName);
        RadioGroup iconGroup = dialogView.findViewById(R.id.iconRadioGroup);
        Spinner spinnerRoom = dialogView.findViewById(R.id.spinnerRoom);

        ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, loadRoomList());
        spinnerRoom.setAdapter(roomAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Add Device")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etDeviceName.getText().toString().trim();
                    int iconResId = R.drawable.ic_device; // default

                    int checkedId = iconGroup.getCheckedRadioButtonId();
                    if (checkedId == R.id.rbLight) {
                        iconResId = R.drawable.ic_light;
                    } else if (checkedId == R.id.rbFan) {
                        iconResId = R.drawable.ic_fan;
                    } else if (checkedId == R.id.rbSensor) {
                        iconResId = R.drawable.ic_sensor;
                    }

                    if (!name.isEmpty()) {
                        String room = spinnerRoom.getSelectedItem().toString();
                        DeviceModel newDevice = new DeviceModel(name, iconResId, room);
                        saveDevice(newDevice);
                        Toast.makeText(this, "Device saved successfully!", Toast.LENGTH_SHORT).show();

                        // ✅ Quay về màn hình chính
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                        // Xoá session và đóng activity
                        DeviceSessionManager.getInstance().clearSession();
                        finishAffinity();
                    } else {
                        Toast.makeText(this, "Please enter a device name", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private List<String> loadRoomList() {
        SharedPreferences prefs = getSharedPreferences("room_prefs", MODE_PRIVATE);
        Set<String> set = prefs.getStringSet("room_list", null);
        List<String> roomList = new ArrayList<>();

        if (set != null) {
            roomList.addAll(set);
        } else {
            // Default room list
            roomList.add("Living Room");
            roomList.add("Bedroom");
            roomList.add("Kitchen");
        }
        return roomList;
    }

    private void saveDevice(DeviceModel device) {
        SharedPreferences prefs = getSharedPreferences("device_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();

        // Load existing device list
        String json = prefs.getString("device_list", "[]");
        Type type = new TypeToken<List<DeviceModel>>(){}.getType();
        List<DeviceModel> deviceList = gson.fromJson(json, type);

        // Add new device
        deviceList.add(device);

        // Save updated list
        String updatedJson = gson.toJson(deviceList);
        editor.putString("device_list", updatedJson);
        editor.apply();
    }

    private void updateStatus(String message) {
        Log.d(TAG, message);
        tvStatus.setText(message);
    }
    @Override
    public void onResume() {
        super.onResume();

        // Make sure adapter is notified properly
        if (wifiAdapter != null) {
            wifiAdapter.notifyDataSetChanged();
        }

        // Force ListView to recalculate its height if needed
        if (listWifiNetworks != null) {
            listWifiNetworks.invalidateViews();
        }
    }


}