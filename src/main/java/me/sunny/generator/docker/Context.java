package me.sunny.generator.docker;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.Alert;
import me.sunny.generator.docker.domain.*;
import me.sunny.generator.docker.enums.DockerDependCondition;
import me.sunny.generator.docker.exception.ApplicationException;
import org.apache.commons.lang3.StringUtils;


public class Context {
    private final static ObjectMapper objectMapper = new ObjectMapper();
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
        alert.showAndWait();
    }
}
