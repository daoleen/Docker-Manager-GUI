package me.sunny.generator.docker;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {
    private static Stage primaryStage;


    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        Main.primaryStage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("project/open-project.fxml"));
        primaryStage.setTitle("Docker-Compose Generator");
        primaryStage.setScene(new Scene(root));
        primaryStage.setOnCloseRequest(event -> exit());
        primaryStage.show();
    }


    public static void exit() {
        primaryStage.close();
        Platform.exit();
        System.exit(0);
    }
}
