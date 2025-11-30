package com.example.esp32haberlesme;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sun.misc.Signal;

import java.io.IOException;

public class Main extends Application {

    private static SignalController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("signal.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 440, 420);
        stage.setTitle("Esp32s3 Sinyal Gönderme");
        stage.setScene(scene);
        stage.show();

        controller = fxmlLoader.getController();

        stage.setOnCloseRequest(event -> {
            try {
                controller.stopThread();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Platform.exit();
            System.exit(0);
        });

    }

    public static void main(String[] args) {
        launch();
    }

}

