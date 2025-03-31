package com.phamthientai.ble_prov;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class LedControlActivity extends AppCompatActivity {

    private EditText edtIp, edtPort;
    private Button btnSave;
    private TextView tvName;
    private ImageView imgIcon;
    private Button btnOn, btnOff;
    private Switch swToggle;

    private MqttClient mqttClient;
    private String topicPub = "esp32/led/control";
    private String topicSub = "esp32/led/status";

    private boolean isLedOn = false;
    private boolean isUpdatingSwitch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_control);

        tvName = findViewById(R.id.tvDeviceName);
        imgIcon = findViewById(R.id.imgDeviceIcon);
        edtIp = findViewById(R.id.edtIp);
        edtPort = findViewById(R.id.edtPort);
        btnSave = findViewById(R.id.btnSaveMqtt);
        btnOn = findViewById(R.id.btnTurnOn);
        btnOff = findViewById(R.id.btnTurnOff);
        swToggle = findViewById(R.id.switchToggle);

        String name = getIntent().getStringExtra("device_name");
        int iconRes = getIntent().getIntExtra("device_icon", R.drawable.ic_device);

        tvName.setText(name);
        imgIcon.setImageResource(iconRes);

        btnSave.setOnClickListener(v -> connectMQTT());

        btnOn.setOnClickListener(v -> publishCommand("ON"));
        btnOff.setOnClickListener(v -> publishCommand("OFF"));

        swToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingSwitch) {
                publishCommand(isChecked ? "ON" : "OFF");
            }
        });

        // Load IP & Port từ SharedPreferences và tự kết nối nếu có
        SharedPreferences prefs = getSharedPreferences("mqtt_config", MODE_PRIVATE);
        String savedIp = prefs.getString("mqtt_ip", "");
        String savedPort = prefs.getString("mqtt_port", "");

        edtIp.setText(savedIp);
        edtPort.setText(savedPort);

        if (!savedIp.isEmpty() && !savedPort.isEmpty()) {
            connectMQTT();
        }
    }

    private void connectMQTT() {
        String ip = edtIp.getText().toString().trim();
        String port = edtPort.getText().toString().trim();

        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
            Toast.makeText(this, "IP hoặc Port không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String serverUri = "tcp://" + ip + ":" + port;

        try {
            mqttClient = new MqttClient(serverUri, MqttClient.generateClientId(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            mqttClient.connect(options);

            mqttClient.subscribe(topicSub);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    runOnUiThread(() ->
                            Toast.makeText(LedControlActivity.this, "Disconnected", Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String msg = new String(message.getPayload());
                    runOnUiThread(() -> {
                        isLedOn = msg.equalsIgnoreCase("ON");
                        isUpdatingSwitch = true;
                        swToggle.setChecked(isLedOn);
                        isUpdatingSwitch = false;

                        if (isLedOn) {
                            imgIcon.setImageResource(R.drawable.ic_light_on);
                        } else {
                            imgIcon.setImageResource(R.drawable.ic_light_off);
                        }
                    });
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            // Lưu IP và Port sau khi kết nối thành công
            SharedPreferences prefs = getSharedPreferences("mqtt_config", MODE_PRIVATE);
            prefs.edit()
                    .putString("mqtt_ip", ip)
                    .putString("mqtt_port", port)
                    .apply();

            Toast.makeText(this, "Connected to " + serverUri, Toast.LENGTH_SHORT).show();

        } catch (MqttException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi kết nối MQTT: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void publishCommand(String command) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            Toast.makeText(this, "MQTT chưa kết nối", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            MqttMessage message = new MqttMessage(command.getBytes());
            mqttClient.publish(topicPub, message);

            isUpdatingSwitch = true;
            if (command.equalsIgnoreCase("ON")) {
                imgIcon.setImageResource(R.drawable.ic_light_on);
                swToggle.setChecked(true);
            } else {
                imgIcon.setImageResource(R.drawable.ic_light_off);
                swToggle.setChecked(false);
            }
            isUpdatingSwitch = false;

        } catch (MqttException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gửi lệnh thất bại", Toast.LENGTH_SHORT).show();
        }
    }
}
