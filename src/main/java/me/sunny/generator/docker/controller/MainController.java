package me.sunny.generator.docker.controller;


import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.Main;


@Slf4j
public class MainController {
    public void openServiceCreateWindow(ActionEvent actionEvent) {
        try {
            Parent serviceCreateWnd = FXMLLoader.load(getClass().getClassLoader().getResource("service/create.fxml"));
            Stage serverCreateStage = new Stage();
            serverCreateStage.initModality(Modality.WINDOW_MODAL);
            serverCreateStage.setTitle("Create a new service");
            serverCreateStage.setScene(new Scene(serviceCreateWnd));
            serverCreateStage.show();
        } catch (IOException ex) {
            log.error("Could not open window for creating a new service: {}", ex.getMessage());
            Context.showNotificationDialog("Error", "Could not open window for creating a new service", Alert.AlertType.ERROR);
        }
    }


    public void quit(ActionEvent actionEvent) {
        Main.exit();
    }


    public void about(ActionEvent actionEvent) {
    }
}
