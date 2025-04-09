package com.phamthientai.ble_prov;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.espressif.provisioning.ESPConstants;
import com.espressif.provisioning.ESPDevice;
import com.espressif.provisioning.ESPProvisionManager;
import com.espressif.provisioning.listeners.QRCodeScanListener;

public class QRScanActivity extends AppCompatActivity {

    private static final String TAG = "QRScanActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 101;

    private CodeScanner codeScanner;
    private CodeScannerView scannerView;
    private TextView tvStatus;
    private Button btnNoQrCode, btnCancel;
    private ESPProvisionManager provisionManager;
    private ESPDevice espDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        // Initialize views
        scannerView = findViewById(R.id.scanner_view);
        tvStatus = findViewById(R.id.tv_status);
        btnNoQrCode = findViewById(R.id.btn_no_qr_code);
        btnCancel = findViewById(R.id.btn_cancel);

        // Initialize Provision Manager
        provisionManager = ESPProvisionManager.getInstance(getApplicationContext());

        // Setup QR scanner
        codeScanner = new CodeScanner(this, scannerView);

        // Check camera permission
        checkCameraPermission();

        // Setup button listeners
        setupListeners();
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> finish());

        btnNoQrCode.setOnClickListener(v -> {
            showManualSetupDialog();
        });
    }

    private void checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startQrCodeScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQrCodeScan();
            } else {
                Toast.makeText(this, "Camera permission is required for QR scanning", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startQrCodeScan() {
        updateStatus("Scanning QR Code...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        provisionManager.scanQRCode(codeScanner, new QRCodeScanListener() {
            @Override
            public void qrCodeScanned() {
                // This is called when QR code is successfully scanned but before device details are extracted
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    updateStatus("QR Code scanning failed: " + e.getMessage());
                });
            }

            @Override
            public void onFailure(Exception e, String strErr) {
                runOnUiThread(() -> {
                    updateStatus("QR Code scanning failed: " + e.getMessage());
                });
            }

            @Override
            public void deviceDetected(ESPDevice device) {
                runOnUiThread(() -> {
                    espDevice = device;
                    updateStatus("QR Code scanned successfully. Device: " + device.getDeviceName());

                    // Set proof of possession if available
                    if (device.getProofOfPossession() != null && !device.getProofOfPossession().isEmpty()) {
                        espDevice.setProofOfPossession(device.getProofOfPossession());
                    }

                    // Store ESP device in session manager
                    DeviceSessionManager.getInstance().setEspDevice(espDevice);

                    // Navigate to WiFi scan activity
                    navigateToWifiScanActivity();
                });
            }
        });
    }

    private void navigateToWifiScanActivity() {
        Intent intent = new Intent(QRScanActivity.this, WifiScanActivity.class);

        // Add basic device information to intent
        intent.putExtra("DEVICE_NAME", espDevice.getDeviceName());
        intent.putExtra("TRANSPORT_TYPE", espDevice.getTransportType().ordinal());
        intent.putExtra("SECURITY_TYPE", espDevice.getSecurityType().ordinal());
        intent.putExtra("POP", espDevice.getProofOfPossession());

        startActivity(intent);

        // Don't finish this activity yet, as user might come back
        // finish();
    }

    private void showManualSetupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manual Setup");
        builder.setMessage("Do you want to set up device manually without QR code?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            // Create a default ESP device (BLE as default transport mode)
            ESPConstants.TransportType transportType = ESPConstants.TransportType.TRANSPORT_BLE;
            ESPConstants.SecurityType securityType = ESPConstants.SecurityType.SECURITY_2;

            espDevice = provisionManager.createESPDevice(transportType, securityType);

            // Let user enter device name and POP
            showDeviceInfoDialog();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeviceInfoDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_manual_device, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Device Information");
        builder.setView(view);

        // You'll need to create this layout with fields for device name and POP
        // Find the EditText fields in the view
        // final EditText etDeviceName = view.findViewById(R.id.et_device_name);
        // final EditText etPop = view.findViewById(R.id.et_pop);

        builder.setPositiveButton("Continue", (dialog, which) -> {
            // String deviceName = etDeviceName.getText().toString();
            // String pop = etPop.getText().toString();

            // if (!deviceName.isEmpty()) {
            //     espDevice.setDeviceName(deviceName);
            // }

            // if (!pop.isEmpty()) {
            //     espDevice.setProofOfPossession(pop);
            // }

            // Store ESP device in session manager
            // DeviceSessionManager.getInstance().setEspDevice(espDevice);

            // Navigate to WiFi scan activity
            // navigateToWifiScanActivity();

            // For now, we'll just navigate without setting these properties
            DeviceSessionManager.getInstance().setEspDevice(espDevice);
            navigateToWifiScanActivity();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateStatus(String message) {
        Log.d(TAG, message);
        tvStatus.setText(message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (codeScanner != null) {
            codeScanner.startPreview();
        }
    }

    @Override
    protected void onPause() {
        if (codeScanner != null) {
            codeScanner.releaseResources();
        }
        super.onPause();
    }
}