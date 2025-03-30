package com.phamthientai.ble_prov;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class LedControlActivity extends AppCompatActivity {

    private MqttClient mqttClient;
    private String topicPub = "esp32/led/control";
    private String topicSub = "esp32/led/status";

    private TextView statusText;
    private ImageView ledIcon;
    private boolean isLightOn = false;

    private EditText etIp, etPort;
    private Button btnSaveIp, btnOn, btnOff;

    private static final String PREFS = "mqtt_prefs";
    private static final String KEY_IP = "mqtt_ip";
    private static final String KEY_PORT = "mqtt_port";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_control);

        TextView tvName = findViewById(R.id.tvDeviceName);
        ledIcon = findViewById(R.id.imgDeviceIcon);
        statusText = findViewById(R.id.tvStatus);
        etIp = findViewById(R.id.etMqttIp);
        etPort = findViewById(R.id.etMqttPort);
        btnSaveIp = findViewById(R.id.btnSaveIp);
        btnOn = findViewById(R.id.btnOn);
        btnOff = findViewById(R.id.btnOff);

        String name = getIntent().getStringExtra("device_name");
        int icon = getIntent().getIntExtra("device_icon", R.drawable.ic_device);
        topicPub = "esp32/" + name + "/control";
        topicSub = "esp32/" + name + "/status";

        tvName.setText(name);
        ledIcon.setImageResource(icon);

        loadSavedMqttConfig();
        connectMQTT();

        btnSaveIp.setOnClickListener(v -> {
            saveMqttConfig();
            connectMQTT();
        });

        btnOn.setOnClickListener(v -> publishCommand("ON"));
        btnOff.setOnClickListener(v -> publishCommand("OFF"));
    }

    private void connectMQTT() {
        String ip = etIp.getText().toString().trim();
        String port = etPort.getText().toString().trim();

        if (ip.isEmpty() || port.isEmpty()) {
            statusText.setText("IP hoặc Port không được để trống");
            return;
        }

        try {
            String serverUri = "tcp://" + ip + ":" + port;
            String clientId = MqttClient.generateClientId();
            mqttClient = new MqttClient(serverUri, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            mqttClient.connect(options);
            mqttClient.subscribe(topicSub);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    runOnUiThread(() -> statusText.setText("Disconnected"));
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String msg = new String(message.getPayload());
                    runOnUiThread(() -> {
                        statusText.setText("Status: " + msg);
                        if (msg.contains("ON")) {
                            isLightOn = true;
                            ledIcon.setColorFilter(Color.YELLOW);
                        } else if (msg.contains("OFF")) {
                            isLightOn = false;
                            ledIcon.setColorFilter(Color.GRAY);
                        }
                    });
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            runOnUiThread(() -> statusText.setText("Connected to " + serverUri));

        } catch (MqttException e) {
            e.printStackTrace();
            runOnUiThread(() -> statusText.setText("MQTT Connect Failed"));
        }
    }

    private void publishCommand(String command) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish(topicPub, new MqttMessage(command.getBytes()));
            } else {
                statusText.setText("MQTT chưa kết nối");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void saveMqttConfig() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_IP, etIp.getText().toString().trim());
        editor.putString(KEY_PORT, etPort.getText().toString().trim());
        editor.apply();
    }

    private void loadSavedMqttConfig() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedIp = prefs.getString(KEY_IP, "192.168.4.1");
        String savedPort = prefs.getString(KEY_PORT, "1883");

        etIp.setText(savedIp);
        etPort.setText(savedPort);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}