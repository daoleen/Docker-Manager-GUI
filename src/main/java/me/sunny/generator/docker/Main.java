package me.sunny.generator.docker;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static javafx.application.Platform.exit;


public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("main.fxml"));
        primaryStage.setTitle("Docker-Compose Generator");
        primaryStage.setScene(new Scene(root));
        primaryStage.setOnCloseRequest(event -> exit());
        primaryStage.show();
    }
}
