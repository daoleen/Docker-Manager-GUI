package me.sunny.generator.docker.controller;


import java.io.IOException;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.Main;
import me.sunny.generator.docker.domain.Composition;
import me.sunny.generator.docker.domain.DockerServiceDescription;


@Slf4j
public class MainController {

    @FXML
    private Label lblProjectName;

    @FXML
    private ListView<Composition> listCompositions;

    @FXML
    private ListView<DockerServiceDescription> listServices;


    @FXML
    public void initialize() {
        if (Context.project == null) {
            Context.showNotificationDialog("Error", "Could not initialize project", Alert.AlertType.ERROR);
            quit();
            return;
        }

        lblProjectName.setText(Context.project.getName());
        listCompositions.setItems(FXCollections.observableArrayList(Context.project.getCompositions()));
        listServices.setItems(FXCollections.observableArrayList(Context.project.getAvailableServices()));
    }


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


    public void quit() {
        Main.exit();
    }


    public void about(ActionEvent actionEvent) {
    }


    public void openServiceComposition(ActionEvent actionEvent) {
        try {
            Parent serviceCompositionWnd = FXMLLoader.load(getClass().getClassLoader().getResource("composition.fxml"));
            Stage serviceCompositionStage = new Stage();
            serviceCompositionStage.initModality(Modality.WINDOW_MODAL);
            serviceCompositionStage.setTitle("Services Composition");
            serviceCompositionStage.setScene(new Scene(serviceCompositionWnd));
            serviceCompositionStage.show();
        } catch (IOException ex) {
            log.error("Could not open window for service composition: {}", ex.getMessage());
            Context.showNotificationDialog("Error", "Could not open window for service composition", Alert.AlertType.ERROR);
        }
    }
}
