module com.example.esp32haberlesme {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires org.eclipse.paho.client.mqttv3;
    requires java.logging;
    requires jdk.unsupported;


    opens com.example.esp32haberlesme to javafx.fxml;
    exports com.example.esp32haberlesme;
}