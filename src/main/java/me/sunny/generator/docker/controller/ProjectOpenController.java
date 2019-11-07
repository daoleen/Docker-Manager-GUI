package me.sunny.generator.docker.controller;


import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;


@Slf4j
public class ProjectOpenController {

    @FXML
    private ListView listRecentProjects;


    public void createNewProject(MouseEvent mouseEvent) {
        log.debug("Create new project clicked");
        Context.initMockedProject();
        openMainWindow();
    }


    public void openExistingProject(MouseEvent mouseEvent) {
        log.debug("openExistingProject clicked");
        Context.initMockedProject();
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
}
