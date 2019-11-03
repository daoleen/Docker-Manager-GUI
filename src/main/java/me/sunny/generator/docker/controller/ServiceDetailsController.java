package me.sunny.generator.docker.controller;


import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.domain.*;
import me.sunny.generator.docker.exception.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;


@Slf4j
public class ServiceDetailsController {
    private DockerServiceDescription dockerServiceDescription;


    @FXML
    private Label lblName;

    @FXML
    private Label lblImage;

    @FXML
    private Label lblRestart;

    @FXML
    private Label lblPorts;

    @FXML
    private Label lblVolumes;

    @FXML
    private Label lblLinks;

    @FXML
    private Label lblEnvironment;

    @FXML
    private Label lblDepend;

    @FXML
    private Label lblHealthCheck;

    @FXML
    private Label lblVersions;


    public void init(String serviceName) {
        try {
            this.dockerServiceDescription = Context.project.findService(serviceName);
        } catch (ResourceNotFoundException e) {
            Context.showNotificationDialog("Service not found", e.toString(), Alert.AlertType.WARNING);
            close();
        }

        String ports = dockerServiceDescription.getService().getPorts().stream()
                .map(DockerPortMapping::toString)
                .collect(Collectors.joining("\n"));

        String volumes = dockerServiceDescription.getService().getVolumes().stream()
                .map(DockerVolumeMapping::toString)
                .collect(Collectors.joining("\n"));

        String links = dockerServiceDescription.getService().getLinks().stream()
                .map(DockerService::getName)
                .collect(Collectors.joining("\n"));

        String environments = dockerServiceDescription.getService().getEnvironment().entrySet().stream()
                .map(entry -> entry.getKey() + " : " + entry.getValue())
                .collect(Collectors.joining("\n"));

        String depends = dockerServiceDescription.getService().getDepends().stream()
                .map(DockerDepend::toString)
                .collect(Collectors.joining("\n"));

        String versions = String.join("\n", dockerServiceDescription.getVersions());

        String healthcheck;

        if (dockerServiceDescription.getService().getHealthcheck() != null && StringUtils.isNotEmpty(dockerServiceDescription.getService().getHealthcheck().getTest())) {
            healthcheck = dockerServiceDescription.getService().getHealthcheck().toString();
        } else {
            healthcheck = "";
        }


        lblName.setText(dockerServiceDescription.getService().getName());
        lblImage.setText(dockerServiceDescription.getService().getImage());
        lblRestart.setText(dockerServiceDescription.getService().getRestart().toString());
        lblPorts.setText(ports);
        lblVolumes.setText(volumes);
        lblLinks.setText(links);
        lblEnvironment.setText(environments);
        lblDepend.setText(depends);
        lblHealthCheck.setText(healthcheck);
        lblVersions.setText(versions);
    }


    public void close() {
        ((Stage) lblName.getScene().getWindow()).close();
    }


    public void editService(ActionEvent actionEvent) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet");
    }


    public void createVersion(ActionEvent actionEvent) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
