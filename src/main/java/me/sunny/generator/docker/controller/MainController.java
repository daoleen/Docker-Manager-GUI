package me.sunny.generator.docker.controller;


import java.io.File;
import java.io.IOException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.Main;
import me.sunny.generator.docker.domain.Composition;
import me.sunny.generator.docker.domain.DockerServiceDescription;
import me.sunny.generator.docker.domain.Host;
import me.sunny.generator.docker.exception.ApplicationException;
import org.apache.commons.io.FileUtils;


@Slf4j
public class MainController {

    @FXML
    private Label lblProjectName;

    @FXML
    private ListView<Composition> listCompositions;

    @FXML
    private ListView<DockerServiceDescription> listServices;

    @FXML
    private ListView<Host> listHosts;


    private Thread reloader = new Thread(() -> {
        while (true) {
            Platform.runLater(this::initData);

            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
            }
        }
    });


    @FXML
    public void initialize() {
        if (Context.project == null) {
            Context.showNotificationDialog("Error", "Could not initialize project", Alert.AlertType.ERROR);
            quit();
            return;
        }

        lblProjectName.setText(Context.project.getName());
        reloader.start();
    }

    private void initData() {
        listCompositions.setItems(FXCollections.observableArrayList(Context.project.getCompositions()));
        listServices.setItems(FXCollections.observableArrayList(Context.project.getAvailableServices()));
        listHosts.setItems(FXCollections.observableArrayList(Context.project.getHosts()));
    }


    public void openServiceCreateWindow(ActionEvent actionEvent) {
        try {
            Parent serviceCreateWnd = FXMLLoader.load(getClass().getClassLoader().getResource("service/create.fxml"));
            Stage serverCreateStage = new Stage();
            serverCreateStage.initModality(Modality.WINDOW_MODAL);
            serverCreateStage.setTitle("Create a new service");
            serverCreateStage.setScene(new Scene(serviceCreateWnd));

            Window sourceWindow = lblProjectName.getScene().getWindow();
            serverCreateStage.setX(sourceWindow.getX());
            serverCreateStage.setY(sourceWindow.getY());

            serverCreateStage.show();
        } catch (IOException ex) {
            log.error("Could not open window for creating a new service: {}", ex.getMessage());
            Context.showNotificationDialog("Error", "Could not open window for creating a new service", Alert.AlertType.ERROR);
        }
    }


    public void quit() {
        reloader.interrupt();
        Main.exit();
    }


    public void about(ActionEvent actionEvent) {
    }


    public void openServiceComposition(ActionEvent actionEvent) {
        openServiceCompositionWindow(null);
    }


    // show service details window on double-click
    public void onMouseClickedOnService(MouseEvent mouseEvent) {
        if (MouseButton.PRIMARY.equals(mouseEvent.getButton()) && 2 == mouseEvent.getClickCount()) {
            openDetailsWindow(listServices.getSelectionModel().getSelectedItem().getService().getName());
        }
    }


    // show composition window on double-click
    public void onMouseClickedOnComposition(MouseEvent mouseEvent) {
        if (MouseButton.PRIMARY.equals(mouseEvent.getButton()) && 2 == mouseEvent.getClickCount()) {
            openServiceCompositionWindow(listCompositions.getSelectionModel().getSelectedItem());
        }
    }


    // show host management window on double-click
    public void onMouseClickedOnHost(MouseEvent mouseEvent) {
        if (MouseButton.PRIMARY.equals(mouseEvent.getButton()) && 2 == mouseEvent.getClickCount()) {
            openHostWindow(listHosts.getSelectionModel().getSelectedItem());
        }
    }


    private void openServiceCompositionWindow(Composition composition) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("composition.fxml"));
            Stage serviceCompositionStage = new Stage();
            serviceCompositionStage.initModality(Modality.WINDOW_MODAL);
            serviceCompositionStage.setTitle("Services Composition");
            serviceCompositionStage.setScene(new Scene(fxmlLoader.load()));

            Window sourceWindow = lblProjectName.getScene().getWindow();
            serviceCompositionStage.setX(sourceWindow.getX());
            serviceCompositionStage.setY(sourceWindow.getY());

            if (composition != null) {
                CompositionController controller = fxmlLoader.<CompositionController>getController();
                controller.selectComposition(composition);
            }

            serviceCompositionStage.show();
        } catch (IOException ex) {
            log.error("Could not open window for service composition: {}", ex.getMessage());
            Context.showNotificationDialog("Error", "Could not open window for service composition", Alert.AlertType.ERROR);
        }
    }


    private void openDetailsWindow(String serviceName) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("service/details.fxml"));
            Stage serviceDetailsStage = new Stage();
            serviceDetailsStage.initModality(Modality.WINDOW_MODAL);
            serviceDetailsStage.setTitle(serviceName);
            serviceDetailsStage.setScene(new Scene(fxmlLoader.load()));
            Window sourceWindow = lblProjectName.getScene().getWindow();
            serviceDetailsStage.setX(sourceWindow.getX());
            serviceDetailsStage.setY(sourceWindow.getY());

            ServiceDetailsController serviceDetailsController = fxmlLoader.<ServiceDetailsController>getController();
            serviceDetailsController.init(serviceName);
            serviceDetailsStage.show();
        } catch (IOException ex) {
            log.error("Could not open window for details of service: {}", ex.getMessage());
            Context.showNotificationDialog("Error", "Could not open window for details of service", Alert.AlertType.ERROR);
        }
    }


    private void openHostWindow(Host host) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("host/host.fxml"));
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Host management");
        Window sourceWindow = lblProjectName.getScene().getWindow();
        stage.setX(sourceWindow.getX());
        stage.setY(sourceWindow.getY());

        try {
            stage.setScene(new Scene(fxmlLoader.load()));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            Context.showNotificationDialog("Error opening host window", e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        HostController hostController = fxmlLoader.<HostController>getController();
        hostController.init(host);
        stage.setOnCloseRequest(event -> {
            log.debug("Host management window closed");
            hostController.quit();
        });
        stage.show();
    }


    public void closeProject(ActionEvent actionEvent) {
        Context.project = null;
        quit();
    }


    public void saveProject(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("JSON file (*.json)", "*.json");
        fileChooser.getExtensionFilters().add(extensionFilter);

        File selectedFile = fileChooser.showSaveDialog(lblProjectName.getScene().getWindow());

        if (selectedFile != null) {
            if (!selectedFile.getName().toLowerCase().endsWith(".json")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".json");
            }

            try {
                Context.serializeProject(selectedFile);
                Context.showNotificationDialog("Saved", "Project successfully saved to file",
                        Alert.AlertType.INFORMATION);
            } catch (ApplicationException e) {
                log.error(e.getMessage(), e);
                Context.showNotificationDialog("Error saving project", e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            log.warn("Nothing was selected");
        }
    }


    public void addNewHost(ActionEvent actionEvent) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("host/create.fxml"));
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Specify a new docker host");
        Window sourceWindow = lblProjectName.getScene().getWindow();
        stage.setX(sourceWindow.getX());
        stage.setY(sourceWindow.getY());

        try {
            stage.setScene(new Scene(fxmlLoader.load()));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            Context.showNotificationDialog("Error creating a new host", e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        stage.showAndWait();
    }


    public void deleteSelectedHost(ActionEvent actionEvent) {
        Host host = listHosts.getSelectionModel().getSelectedItem();

        if (host != null) {
            Context.project.getHosts().remove(host);
            log.info("Host {} has been deleted");
            initData();
        }
    }
}
