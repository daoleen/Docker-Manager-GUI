package me.sunny.generator.docker.controller;


import java.util.*;
import java.util.stream.Collectors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.domain.*;
import me.sunny.generator.docker.exception.ApplicationException;
import me.sunny.generator.docker.exception.ContainerStartException;
import me.sunny.generator.docker.exception.ResourceNotFoundException;
import me.sunny.generator.docker.service.DockerContainerRunner;
import me.sunny.generator.docker.service.DockerContainerService;
import me.sunny.generator.docker.util.ServiceFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;


@Slf4j
public class HostController {
    private Host host;
    private DockerContainerService dockerContainerService;
    private DockerContainerRunner dockerContainerRunner;


    @FXML
    private Label lblTitle;

    @FXML
    private ListView<DockerContainer> listRunningContainers;

    @FXML
    private ListView<Composition> listAvailableCompositions;

    @FXML
    private TableView<DockerServiceConcreted> tblSelectedServices;

    @FXML
    private TableColumn<DockerServiceConcreted, String> tblSelectedServicesColService;

    @FXML
    private TableColumn<DockerServiceConcreted, String> tblSelectedServicesColVersion;


    private Thread reloader = new Thread(() -> {
        while (true) {
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
            }

            Platform.runLater(() -> {
                initRunningContainers();
                initTblSelectedServices();
                initAvailableServices();
            });
        }
    });


    public void quit() {
        reloader.interrupt();
    }


    public void init(Host host) {
        this.host = host;
        lblTitle.setText(String.format("Host %s overview", host.getAddress()));
        listAvailableCompositions.setItems(FXCollections.observableArrayList(Context.project.getCompositions()));

        DefaultDockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(this.host.getAddress())
                .build();

        DockerClient dockerClient = DockerClientBuilder.getInstance(clientConfig).build();
        dockerContainerService = ServiceFactory.getDockerContainerService(dockerClient);
        dockerContainerRunner = ServiceFactory.getDockerContainerRunner(dockerContainerService);
        reloader.start();
    }


    private void initRunningContainers() {
        List<DockerContainer> runningContainers = dockerContainerService.getContainers(false);
        listRunningContainers.setItems(FXCollections.observableList(runningContainers));
    }


    private void initTblSelectedServices() {
        tblSelectedServices.setEditable(true);
        tblSelectedServicesColService.setCellValueFactory(cellData -> {
            try {
                String name = Context.project.findService(cellData.getValue().getServiceId()).getService().getName();
                return new SimpleStringProperty(name);
            } catch (ResourceNotFoundException e) {
                log.error(e.getMessage());
            }
            return null;
        });
        tblSelectedServicesColVersion.setCellValueFactory(new PropertyValueFactory<>("version"));

        Callback<TableColumn<DockerServiceConcreted, String>, TableCell<DockerServiceConcreted, String>>
                cellVersionCallback = new Callback<TableColumn<DockerServiceConcreted, String>, TableCell<DockerServiceConcreted, String>>() {
            @Override
            public TableCell<DockerServiceConcreted, String> call(TableColumn<DockerServiceConcreted, String> param) {
                return new TableCell<DockerServiceConcreted, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty) {
                            setGraphic(null);
                        } else {
                            ComboBox<String> comboBox = new ComboBox<>();
                            DockerServiceConcreted dockerServiceConcretedSelected = getTableView().getItems().get(getIndex());
                            UUID selectedService = null;
                            try {
                                selectedService =
                                        Context.project.findService(dockerServiceConcretedSelected.getServiceId()).getService().getId();
                            } catch (ResourceNotFoundException e) {
                                log.error(e.getMessage());
                                return;
                            }

                            List<String> versions = null;
                            try {
                                versions = Context.project.findService(selectedService).getVersions();
                            } catch (ResourceNotFoundException e) {
                                log.error(e.getMessage(), e);
                                versions = Collections.emptyList();
                            }

                            comboBox.setItems(FXCollections.observableList(versions));
                            comboBox.setOnAction(event -> {
                                dockerServiceConcretedSelected.setVersion(comboBox.getSelectionModel().getSelectedItem());
                            });

                            UUID finalSelectedService = selectedService;
                            tblSelectedServices.getItems()
                                    .stream()
                                    .filter(s -> Objects.equals(s.getServiceId(), finalSelectedService))
                                    .findFirst()
                                    .ifPresent(serviceConcreted -> {
                                        String version = serviceConcreted.getVersion();
                                        comboBox.getSelectionModel().select(version);
                                    });

                            setGraphic(comboBox);
                        }
                    }
                };
            }
        };

        tblSelectedServicesColVersion.setCellFactory(cellVersionCallback);
        tblSelectedServicesColVersion.setOnEditCommit(event -> {
            String version = event.getNewValue();
            event.getRowValue().setVersion(version);
        });

    }


    private void initAvailableServices() {
        List<DockerServiceConcreted> availableServicesConcreted = Context.project.getAvailableServices().stream()
                .map(serviceDescription -> new DockerServiceConcreted(serviceDescription.getService().getId(),
                        serviceDescription.getVersions().get(serviceDescription.getVersions().size() - 1)))
                .collect(Collectors.toList());

        ObservableList<DockerServiceConcreted> observableList = FXCollections.observableList(availableServicesConcreted);
        tblSelectedServices.setItems(observableList);
    }


    public void stopContainer(ActionEvent actionEvent) {
        DockerContainer container = listRunningContainers.getSelectionModel().getSelectedItem();

        if (container != null) {
            stopContainer(container);
        }
    }


    private void stopContainer(DockerContainer container) {
        dockerContainerService.stop(container.getId());
        dockerContainerService.remove(container.getId());
    }


    public void startService(ActionEvent actionEvent) {
        DockerServiceConcreted serviceConcreted = tblSelectedServices.getSelectionModel().getSelectedItem();

        if (serviceConcreted != null) {
            try {
                startService(serviceConcreted);
            } catch (ApplicationException | ResourceNotFoundException | ContainerStartException e) {
                log.error(e.getMessage());
                Context.showNotificationDialog("Error starting container", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }


    public void startService(DockerServiceConcreted serviceConcreted)
            throws ApplicationException, ContainerStartException, ResourceNotFoundException
    {
        DockerServiceDescription service = null;

        try {
            service = Context.project.findService(serviceConcreted.getServiceId());
        } catch (ResourceNotFoundException e) {
            throw new ApplicationException(String.format("Could not find service %s", serviceConcreted.getServiceId()));
        }

        if (CollectionUtils.isEmpty(service.getVersions())) {
            throw new ApplicationException(String.format("Could not start service %s because it has no any version",
                    service.getService().getName()));
        }

        if (StringUtils.isBlank(serviceConcreted.getVersion())) {
            throw new ApplicationException(String.format("Could not start service %s. Service version is not selected",
                    serviceConcreted.getVersion()));
        }

        dockerContainerRunner.startService(service.getService(), serviceConcreted.getVersion(), null);
    }


    public void startComposition(ActionEvent actionEvent) {
        Composition composition = listAvailableCompositions.getSelectionModel().getSelectedItem();

        if (composition != null) {
            new Thread(() -> {
                composition.getServices().forEach(serviceConcreted -> {
                    try {
                        startService(serviceConcreted);
                    } catch (ApplicationException | ResourceNotFoundException | ContainerStartException e) {
                        log.error(e.getMessage());
                        Context.showNotificationDialog("Error starting container", e.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            }).start();
        }
    }


    public void stopComposition(ActionEvent actionEvent) {
        Composition composition = listAvailableCompositions.getSelectionModel().getSelectedItem();

        if (composition != null) {
            new Thread(() -> {
                composition.getServices().forEach(serviceConcreted -> {
                    DockerServiceDescription service;

                    try {
                        service = Context.project.findService(serviceConcreted.getServiceId());
                    } catch (ResourceNotFoundException e) {
                        Context.showNotificationDialog("Service not found", String.format("Could not find service %s", serviceConcreted.getServiceId()), Alert.AlertType.WARNING);
                        return;
                    }

                    Optional<DockerContainer> container = dockerContainerService.getByName(service.getService().getName());

                    if (container.isPresent()) {
                        stopContainer(container.get());
                    } else {
                        Context.showNotificationDialog("Container not found", String.format("Could not find container for service %s", service.getService().getName()), Alert.AlertType.WARNING);
                    }
                });
            }).start();
        }
    }
}
