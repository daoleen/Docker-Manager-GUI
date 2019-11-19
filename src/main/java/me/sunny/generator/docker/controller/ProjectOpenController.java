package me.sunny.generator.docker.controller;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.exception.ApplicationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;


@Slf4j
public class ProjectOpenController {
    private final String recentFileLocation = System.getProperty("user.home") + File.separator + ".docker-compose-generator" + File.separator + "recent.txt";
    private final File recentFile = new File(recentFileLocation);


    @FXML
    private ListView<String> listRecentProjects;


    @FXML
    public void initialize() {
        initRecentList();
    }


    private void initRecentList() {
        try {
            List<String> recentProjects = FileUtils.readLines(recentFile, "UTF-8");
            listRecentProjects.setItems(FXCollections.observableList(recentProjects));
        } catch (IOException e) {
            log.debug("No recent projects found");
        }
    }


    // open selected recent project on double-click
    public void onMouseClickedOnRecentFile(MouseEvent mouseEvent) {
        if (MouseButton.PRIMARY.equals(mouseEvent.getButton()) && 2 == mouseEvent.getClickCount()) {
            String fileLocationStr = listRecentProjects.getSelectionModel().getSelectedItem();

            if (StringUtils.isNotBlank(fileLocationStr)) {
                File projectFile = new File(fileLocationStr);
                openProject(projectFile);
            }
        }
    }


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

        if (projectFile != null) {
            openProject(projectFile);
        }
    }


    private void openProject(File projectFile) {
        try {
            Context.deserializeProject(projectFile);
        } catch (ApplicationException e) {
            Context.showNotificationDialog("Error opening project", e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        saveProjectToRecent(projectFile.getAbsolutePath());
        openMainWindow();
    }


    private void saveProjectToRecent(String absolutePath) {
        String recent = readRecent().replaceAll(Pattern.quote(absolutePath.concat("\n")), "");
        recent = absolutePath.concat("\n").concat(recent);
        saveRecent(recent);
    }


    private String readRecent() {
        String recent;

        try {
            recent = FileUtils.readFileToString(recentFile, "UTF-8");
        } catch (IOException e) {
            log.debug("Creating a new recent file");
            recent = "";
        }
        return recent;
    }


    private void saveRecent(String content) {
        try {
            FileUtils.writeStringToFile(recentFile, content, "UTF-8");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            Context.showNotificationDialog("Error", e.getMessage(), Alert.AlertType.WARNING);
        }
    }


    private void openMainWindow() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("main.fxml"));
        Stage windowStage = new Stage();
        windowStage.initModality(Modality.APPLICATION_MODAL);
        windowStage.setTitle("Docker Compose Generator: " + Context.project.getName());
        Window sourceWindow = listRecentProjects.getScene().getWindow();
        windowStage.setX(sourceWindow.getX());
        windowStage.setY(sourceWindow.getY());

        try {
            windowStage.setScene(new Scene(fxmlLoader.load()));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            Context.showNotificationDialog("Error opening main window", e.getMessage(), Alert.AlertType.ERROR);
        }

        final MainController controller = fxmlLoader.<MainController>getController();
        windowStage.setOnCloseRequest(event -> {
            log.debug("Main window closed");
            controller.quit();
        });
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
        Window sourceWindow = listRecentProjects.getScene().getWindow();
        dialogStage.setX(sourceWindow.getX());
        dialogStage.setY(sourceWindow.getY());

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


    public void removeRecentProject(ActionEvent actionEvent) {
        String recentToRemove = listRecentProjects.getSelectionModel().getSelectedItem();
        saveRecent(readRecent().replaceAll(recentToRemove.concat("\n"), ""));
        initRecentList();
    }
}
