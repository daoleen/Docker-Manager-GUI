package me.sunny.generator.docker.controller;


import java.io.IOException;
import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Modality;
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
    private Label lblBuild;

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

        String links = Context.project.findAllServicesByIds(dockerServiceDescription.getService().getLinks()).stream()
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
        lblBuild.setText(dockerServiceDescription.getService().getBuildPath());
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
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("service/update.fxml"));
            Stage serviceUpdateStage = new Stage();
            serviceUpdateStage.initModality(Modality.WINDOW_MODAL);
            serviceUpdateStage.setTitle("Update " + dockerServiceDescription.getService().getName());
            serviceUpdateStage.setScene(new Scene(fxmlLoader.load()));
            ServiceUpdateController serviceUpdateController = fxmlLoader.getController();
            serviceUpdateController.init(dockerServiceDescription.getService().getName());
            serviceUpdateStage.show();
            close();
        } catch (IOException ex) {
            log.error("Could not open window for update service: {}", ex.getMessage());
            Context.showNotificationDialog("Error", "Could not open window for update service", Alert.AlertType.ERROR);
        }
    }


    public void createVersion(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("version/create.fxml"));
            Stage versionCreateStage = new Stage();
            versionCreateStage.initModality(Modality.WINDOW_MODAL);
            versionCreateStage.setTitle("Create new version for " + dockerServiceDescription.getService().getName());
            versionCreateStage.setScene(new Scene(fxmlLoader.load()));
            VersionCreateController versionCreateController = fxmlLoader.<VersionCreateController>getController();
            versionCreateController.init(dockerServiceDescription.getService().getName(), this);
            versionCreateStage.show();
        } catch (IOException ex) {
            log.error("Could not open window for details of service: {}", ex.getMessage());
            Context.showNotificationDialog("Error", "Could not open window for details of service", Alert.AlertType.ERROR);
        }
    }


    public void refreshVersions() {
        String versions = String.join("\n", dockerServiceDescription.getVersions());
        lblVersions.setText(versions);
    }


    public void deleteService(ActionEvent actionEvent) {
        Context.project.getAvailableServices().remove(dockerServiceDescription);
        close();
    }
}
