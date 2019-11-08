package me.sunny.generator.docker.controller;


import java.io.File;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.exception.ApplicationException;
import org.apache.commons.lang3.StringUtils;


@Slf4j
public class ProjectOpenController {

    @FXML
    private ListView listRecentProjects;


    public void createNewProject(MouseEvent mouseEvent) {
        Pair<Boolean, String> resultChoosingProjectName = chooseProjectName();

        // if result is false then do nothing
        if (!resultChoosingProjectName.getKey()) {
            log.debug("User cancelled creating a new project");
            return;
        }

        if (StringUtils.isEmpty(resultChoosingProjectName.getValue())) {
            Context.showNotificationDialog("Error creating a new project", "Project name is not specified", Alert.AlertType.WARNING);
            return;
        }

        Context.createEmptyProject(resultChoosingProjectName.getValue());
        openMainWindow();
    }


    public void openExistingProject(MouseEvent mouseEvent) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("JSON file (*.json)", "*.json");
        fileChooser.getExtensionFilters().add(extensionFilter);

        File projectFile = fileChooser.showOpenDialog(listRecentProjects.getScene().getWindow());

        try {
            Context.deserializeProject(projectFile);
        } catch (ApplicationException e) {
            Context.showNotificationDialog("Error opening project", e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        openMainWindow();
    }


    private void openMainWindow() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("main.fxml"));
        Stage windowStage = new Stage();
        windowStage.initModality(Modality.APPLICATION_MODAL);
        windowStage.setTitle("Docker Compose Generator: " + Context.project.getName());
        try {
            windowStage.setScene(new Scene(fxmlLoader.load()));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            Context.showNotificationDialog("Error opening main window", e.getMessage(), Alert.AlertType.ERROR);
        }

        MainController controller = fxmlLoader.<MainController>getController();
        windowStage.show();

        close();
    }


    private void close() {
        ((Stage) listRecentProjects.getScene().getWindow()).close();
    }


    private Pair<Boolean, String> chooseProjectName() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("dialog.fxml"));
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Enter new project name");
        try {
            dialogStage.setScene(new Scene(fxmlLoader.load()));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            Context.showNotificationDialog("Error choosing new project name", e.getMessage(), Alert.AlertType.ERROR);
        }
        DialogController dialogController = fxmlLoader.<DialogController>getController();
        dialogController.init("Please enter new composition name here", "");
        dialogStage.showAndWait();

        return new Pair<>(!dialogController.isCancelled(), dialogController.getContent());
    }
}
