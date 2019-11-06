package me.sunny.generator.docker;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import javafx.scene.control.Alert;
import me.sunny.generator.docker.domain.DockerDepend;
import me.sunny.generator.docker.domain.DockerService;
import me.sunny.generator.docker.domain.DockerServiceDescription;
import me.sunny.generator.docker.domain.Project;
import me.sunny.generator.docker.enums.DockerDependCondition;


public class Context {
    public static Project project;

    static {
        project = new Project();


        // TODO: mocked
        DockerServiceDescription service1 = new DockerServiceDescription(new DockerService("service1", "img", null, null, null, null, null, null, null, null), Arrays.asList("s1-v1", "s1-v2", "s1-v3"));
        DockerServiceDescription service2 = new DockerServiceDescription(new DockerService("service2", "img", null, null, null, null, null, null, null, null), Arrays.asList("s2-v1", "s2-v2", "s2-v3"));
        DockerServiceDescription service3 = new DockerServiceDescription(new DockerService("service3", "img", null, null, null, null, null, null, null, null), Arrays.asList("s3-v1", "s3-v2", "s3-v3"));
        DockerServiceDescription service4 = new DockerServiceDescription(new DockerService("service4", "img", null, null, null, null, null, null, null, null), Arrays.asList("s4-v1", "s4-v2", "s4-v3"));
        DockerServiceDescription service5 = new DockerServiceDescription(new DockerService("service5", "img", null, null, null, null, null, null, null, null), Arrays.asList("s5-v1", "s5-v2", "s5-v3"));

        HashSet<DockerDepend> docker5depends = new HashSet<>();
        docker5depends.add(new DockerDepend(service1.getService(), DockerDependCondition.SERVICE_HEALTHY));
        docker5depends.add(new DockerDepend(service3.getService(), DockerDependCondition.SERVICE_HEALTHY));
        service5.getService().setDepends(docker5depends);

        project.setAvailableServices(new HashSet<>(Arrays.asList(service1, service2, service3, service4, service5)));
    }



    public static void showNotificationDialog(String title, String description, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(description);
        alert.showAndWait();
    }
}
