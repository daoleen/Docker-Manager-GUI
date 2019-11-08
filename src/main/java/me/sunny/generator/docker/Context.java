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


    // TODO: mocked
    public static void initMockedProject() {
        project = new Project();
        project.setName("GENERATOR-DOCKER-PROJECT");
        Set<Object> emptySet = Collections.emptySet();

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


        Composition composition1 = new Composition("composition 1", new HashSet<>(Arrays.asList(new DockerServiceConcreted(service2.getService(), service2.getVersions().get(0)), new DockerServiceConcreted(service4.getService(), service4.getVersions().get(0)))));
        Composition composition2 = new Composition("composition 2", new HashSet<>(Arrays.asList(new DockerServiceConcreted(service1.getService(), service1.getVersions().get(0)), new DockerServiceConcreted(service3.getService(), service3.getVersions().get(0)), new DockerServiceConcreted(service5.getService(), service5.getVersions().get(0)))));
        Composition composition3 = new Composition("composition 3", new HashSet<>(Arrays.asList(new DockerServiceConcreted(service2.getService(), service2.getVersions().get(0)), new DockerServiceConcreted(service5.getService(), service5.getVersions().get(service5.getVersions().size() - 1)))));
        project.setCompositions(new HashSet<>(Arrays.asList(composition1, composition2, composition3)));
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
