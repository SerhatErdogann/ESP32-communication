package com.example.esp32haberlesme;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.*;

public class SignalController {

    @FXML
    private ChoiceBox<String> colorChoiceBox;
    @FXML
    private TextArea textArea;

    private static final String AIO_KEY = "AIO_KEY";
    private static final String USERNAME = "USERNAME";
    //1883 şifresiz mqtt portu
    private static final String MQTT_SERVER = "tcp://io.adafruit.com:1883";
    private static final String TOPIC= USERNAME + "/feeds/light";
    private static final String INFO_TOPIC= USERNAME + "/feeds/infolight";
    public static Boolean running=true;

    private static Queue<String> queue = new LinkedList<>();
    private MqttClient client;

    static {
        Logger.getLogger("org.eclipse.paho.client.mqttv3").setLevel(Level.OFF);
    }

    @FXML
    public void initialize() {
        colorChoiceBox.getItems().addAll("Beyaz", "Kirmizi", "Yesil", "Sari");
        colorChoiceBox.setValue("Beyaz");

        Thread connectionThread = new Thread(() -> {
            while (running) {
                try {
                    if (client == null || !client.isConnected()) {
                        connect();
                    }
                    Thread.sleep(1500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        connectionThread.setDaemon(true);
        connectionThread.start();
    }
    private void connect(){
        try{
        String clientId = "JavaFXClient-"+System.currentTimeMillis();

        client =new MqttClient(MQTT_SERVER, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();

        options.setPassword(AIO_KEY.toCharArray());
        options.setUserName(USERNAME);
        options.setCleanSession(true);
        //baglantı kontrolu için ping süresi
        options.setKeepAliveInterval(1);
        //bağlantı koparsa otomatik tekrar baglanmak için
        options.setAutomaticReconnect(true);
        //broker(yani adafruit)'a bağlan sonra publish ile ilgili kanala mesaj gönder.
        client.connect(options);

        client.subscribe(INFO_TOPIC, 0);

            //brokerdan herhangı bır bıldırım/mesaj geldıgınde callback calısır.
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Platform.runLater(() ->
                            textArea.appendText("MQTT bağlantısı koptu."+"\n"));
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String msg = new String(message.getPayload());

                    if (topic.equals(INFO_TOPIC)) {
                        Platform.runLater(() ->
                                textArea.appendText("Esp32'den gelen mesaj: "+msg+"\n"));
                    }
                }
                //qos 1ve2 de garanti gittigini göstermek için kullanılır.
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            Platform.runLater(() ->
        textArea.appendText("Connected to the MQTT broker at: " + MQTT_SERVER+"\n")
        );
        //bağlantı saglandıktan sonra ilk olarak queue da veri varsa onu yollayacak
        if(!queue.isEmpty()) sendFromQueue();

    }catch (Exception e){

        }
    }

    @FXML
    private void onLightOnClicked() {
        sendCommand("on");
    }

    @FXML
    private void onLightOffClicked() {
        sendCommand("off");
    }

    private void sendCommand(String action) {
        try {
            String selectedColor = colorChoiceBox.getValue().toLowerCase();
            String valueToSend = action.equals("on") ? action + "-" + selectedColor : "off";

            if (client == null || !client.isConnected()) {
                queue.add(valueToSend);
                Platform.runLater(() ->
                textArea.appendText("MQTT bağlantısı yok. Mesaj kuyruğa alındı: "+valueToSend+"\n"));
                return;
            }
            if(!queue.isEmpty()){
                queue.add(valueToSend);
                Platform.runLater(() ->
                textArea.appendText("Sırada gönderilecek komutlar var. "+ valueToSend+" komutun queue'ya alındı."+"\n"));
                return;
            }
            MqttMessage message = new MqttMessage(valueToSend.getBytes());
            message.setQos(0);
            //publish: mesaj gonderme(send gibi). ilgili kanalı bulup mesaj yolluyorsun
            client.publish(TOPIC, message);
            Platform.runLater(() ->
            textArea.appendText("Gönderilen komut: " + valueToSend+"\n"));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private void sendFromQueue() {
        if (client == null || !client.isConnected()) return;

        while (!queue.isEmpty()) {
            String msg = queue.poll();
            try {
                MqttMessage mqttMsg = new MqttMessage(msg.getBytes());
                mqttMsg.setQos(0);
                client.publish(TOPIC, mqttMsg);
                Platform.runLater(() ->
                textArea.appendText("Kuyruktan Mesaj Gönderildi: " + msg+"\n"));
                Thread.sleep(1000);
            } catch (Exception e) {
                // Bağlantı yine koptuysa mesaj kaybolmasın
                queue.add(msg);
                break;
            }
        }
    }

    public void stopThread() {
        running = false;
        if (client != null) {
            try {
                client.disconnect();
                client.close();
            } catch (Exception ignored) {}
        }
    }
}
