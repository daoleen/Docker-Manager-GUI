package me.sunny.generator.docker.controller;


import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.exception.ResourceNotFoundException;


public class VersionCreateController {
    private String serviceName;
    private ServiceDetailsController serviceDetailsController;

    @FXML
    private Label lblServiceName;

    @FXML
    private TextField txtVersion;


    public void init(String serviceName, ServiceDetailsController serviceDetailsController) {
        this.serviceName = serviceName;
        this.serviceDetailsController = serviceDetailsController;

        Platform.runLater(() -> {
            lblServiceName.setText(serviceName);
            lblServiceName.requestFocus();
        });
    }


    public void create(ActionEvent actionEvent) {
        try {
            Context.project.findService(serviceName)
                    .getVersions()
                    .add(txtVersion.getText());
        } catch (ResourceNotFoundException e) {
            Context.showNotificationDialog("Error", e.toString(), Alert.AlertType.ERROR);
        }

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("service/details.fxml"));
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        serviceDetailsController.refreshVersions();
        close();
    }


    public void cancel(ActionEvent actionEvent) {
        close();
    }


    private void close() {
        ((Stage) lblServiceName.getScene().getWindow()).close();
    }
}
