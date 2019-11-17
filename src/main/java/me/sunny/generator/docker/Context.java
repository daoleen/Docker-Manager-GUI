package me.sunny.generator.docker;


import java.io.File;
import java.io.IOException;
import java.util.Observable;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.Alert;
import me.sunny.generator.docker.domain.Project;
import me.sunny.generator.docker.exception.ApplicationException;


public class Context {
    private final static ObjectMapper objectMapper = new ObjectMapper();
    public final static HostStatusObservable HOST_STATUS_OBSERVABLE = new HostStatusObservable();
    public static Project project;


    public static void createEmptyProject(String projectName) {
        project = new Project(projectName);
    }


    public static void serializeProject(File outputFile) throws ApplicationException {
        if (project == null) {
            throw new ApplicationException("Project is not initialized");
        }

        try {
            objectMapper.writeValue(outputFile, project);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }


    public static void deserializeProject(File projectFile) throws ApplicationException {
        if (!projectFile.exists()) {
            throw new ApplicationException("Specified project file is not found");
        }

        try {
            Context.project = objectMapper.readValue(projectFile, Project.class);
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }



    public static void showNotificationDialog(String title, String description, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(description);
        alert.setResizable(true);
        alert.showAndWait();
    }


    public static class HostStatusObservable extends Observable {
        @Override
        public void notifyObservers(Object arg) {
            setChanged();
            super.notifyObservers(arg);
        }
    }
}
