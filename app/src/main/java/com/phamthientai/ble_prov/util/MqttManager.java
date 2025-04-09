// MqttManager.java - class tiện ích để publish MQTT command
package com.phamthientai.ble_prov.util;

import android.content.Context;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttManager {

    public static void publishCommand(Context context, String ip, String port, String command) {
        String serverUri = "tcp://" + ip + ":" + port;

        new Thread(() -> {
            try {
                MqttClient client = new MqttClient(serverUri, MqttClient.generateClientId(), new MemoryPersistence());
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                client.connect(options);

                client.publish("esp32/led/control", new MqttMessage(command.getBytes()));
                client.disconnect();

                ((android.app.Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Đã gửi lệnh: " + command, Toast.LENGTH_SHORT).show()
                );

            } catch (MqttException e) {
                e.printStackTrace();
                ((android.app.Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Lỗi MQTT: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}