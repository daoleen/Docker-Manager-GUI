package me.sunny.generator.docker;


import javafx.scene.control.Alert;
import me.sunny.generator.docker.domain.Project;


public class Context {
    public static Project project;

    static {
        project = new Project();
    }



    public static void showNotificationDialog(String title, String description, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(description);
        alert.showAndWait();
    }
}
