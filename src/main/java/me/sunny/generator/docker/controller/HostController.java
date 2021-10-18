package me.sunny.generator.docker.controller;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
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
    private DockerClient dockerClient;
    private DockerContainerService dockerContainerService;
    private DockerContainerRunner dockerContainerRunner;
    private volatile boolean connectionInitialized;


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

    @FXML
    private Label lblStatus;


    private Thread reloader = new Thread(() -> {
        while (true) {
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                break;
            }

            if (connectionInitialized) {
                Platform.runLater(() -> {
                    initRunningContainers();
                    initTblSelectedServices();
                    initAvailableServices();
                });
            }
        }
    }, "reloader_" + Math.random());


    public void quit() {
        reloader.interrupt();
        if (dockerClient != null) {
            try {
                dockerClient.close();
            } catch (IOException e) {
                log.warn(e.getMessage());
            }
        }
    }


    public void init(Host host) {
        this.host = host;
        lblTitle.setText(String.format("Host %s overview", host.getAddress()));
        listAvailableCompositions.setItems(FXCollections.observableArrayList(Context.project.getSortedCompositionsList()));

        initDocker();
        initHostStatusObserver();

        reloader.start();
    }


    private void initDocker() {
        DefaultDockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(host.getAddress())
                .withDockerTlsVerify(host.isUseSSL())
                .withDockerCertPath(host.getCertificatesPath())
                .build();

        DockerCmdExecFactory factory = new NettyDockerCmdExecFactory().withConnectTimeout(100000);
        dockerClient = DockerClientBuilder.getInstance(clientConfig)
                .withDockerCmdExecFactory(factory)
                .build();
        dockerContainerService = ServiceFactory.getDockerContainerService(dockerClient, host);
        dockerContainerRunner = ServiceFactory.getDockerContainerRunner(dockerContainerService);

        pingDocker(dockerClient);
    }


    private void pingDocker(DockerClient dockerClient) {
        new Thread(() -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();

            Future<?> pingTask = executor.submit(() -> {
                Platform.runLater(() -> {
                    lblStatus.setText("Connecting to docker host: " + this.host.getAddress());
                });

                dockerClient.pingCmd().exec();
                connectionInitialized = true;

                Platform.runLater(() -> {
                    lblStatus.setText("Docker host connected");
                });
            });

            try {
                pingTask.get(30, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                Platform.runLater(() -> {
                    log.error("Could not connect to docker daemon", ex);
                    Context.showNotificationDialog("Could not connect to server",
                            "Could not connect to docker daemon", Alert.AlertType.ERROR);
                    close();
                });
            } finally {
                executor.shutdown();
            }
        }).start();
    }


    private void initHostStatusObserver() {
        Observer hostStatusObserver = ((o, arg) -> {
            Platform.runLater(() -> lblStatus.setText(arg.toString()));
        });

        Context.HOST_STATUS_OBSERVABLE.addObserver(hostStatusObserver);
    }


    private void initRunningContainers() {
        List<DockerContainer> runningContainers = dockerContainerService.getContainers(false);
        listRunningContainers.setItems(FXCollections.observableList(runningContainers));
        listRunningContainers.refresh();
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
                .map(serviceDescription -> {
                    String version = "";

                    // If user has selected a not latest version then show the selected version
                    if (!tblSelectedServices.getItems().isEmpty()) {
                        for (DockerServiceConcreted service : tblSelectedServices.getItems()) {
                            if (service.getServiceId().equals(serviceDescription.getService().getId())) {
                                version = service.getVersion();
                                break;
                            }
                        }
                    }

                    // User doesn't select a version so put the latest one
                    if (StringUtils.isBlank(version)) {
                        version = serviceDescription.getVersions().get(serviceDescription.getVersions().size() - 1);
                    }

                    return new DockerServiceConcreted(serviceDescription.getService().getId(), version);
                })
                .collect(Collectors.toList());

        ObservableList<DockerServiceConcreted> observableList = FXCollections.observableList(availableServicesConcreted);
        tblSelectedServices.setItems(observableList);
    }


    public void stopContainer(ActionEvent actionEvent) {
        DockerContainer container = listRunningContainers.getSelectionModel().getSelectedItem();

        if (container != null) {
            new Thread(() -> {
                stopContainer(container);
            }).start();
        }
    }


    private void stopContainer(DockerContainer container) {
        Context.HOST_STATUS_OBSERVABLE.notifyObservers("Stopping container: " + container);
        dockerContainerService.stop(container.getId());
        dockerContainerService.remove(container.getId());
        Context.HOST_STATUS_OBSERVABLE.notifyObservers(String.format("Container `%s` stopped", container));
    }


    public void startService(ActionEvent actionEvent) {
        DockerServiceConcreted serviceConcreted = tblSelectedServices.getSelectionModel().getSelectedItem();

        if (serviceConcreted != null) {
                new Thread(() -> {
                    try {
                        startService(serviceConcreted, false);
                    } catch (ApplicationException | ResourceNotFoundException | ContainerStartException e) {
                        log.error(e.getMessage());
                        Platform.runLater(() -> {
                            Context.showNotificationDialog("Error starting container", e.getMessage(), Alert.AlertType.ERROR);
                        });
                    }
                }).start();
        }
    }


    public void startServiceIndependently(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Are you sure?");
        alert.setHeaderText("Are you sure you want to start that service independently?");
        alert.setContentText("If you start the service independently all the services\non which that service depends won't be started!");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().equals(ButtonType.OK)) {
            DockerServiceConcreted serviceConcreted = tblSelectedServices.getSelectionModel().getSelectedItem();

            if (serviceConcreted != null) {
                new Thread(() -> {
                    try {
                        startService(serviceConcreted, true);
                    } catch (ApplicationException | ResourceNotFoundException | ContainerStartException e) {
                        log.error(e.getMessage());
                        Platform.runLater(() -> {
                            Context.showNotificationDialog("Error starting container", e.getMessage(),
                                    Alert.AlertType.ERROR);
                        });
                    }
                }).start();
            }
        }
    }


    public void startService(DockerServiceConcreted serviceConcreted, boolean startIndependently)
            throws ApplicationException, ContainerStartException, ResourceNotFoundException
    {
        DockerServiceDescription service;

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

        dockerContainerRunner.startService(service.getService(), serviceConcreted.getVersion(), null, startIndependently);
    }


    public void startComposition(ActionEvent actionEvent) {
        Composition composition = listAvailableCompositions.getSelectionModel().getSelectedItem();

        if (composition != null) {
            new Thread(() -> {
                Context.HOST_STATUS_OBSERVABLE.notifyObservers("Starting composition " + composition.getName());

                composition.getServices().forEach(serviceConcreted -> {
                    try {
                        startService(serviceConcreted, false);
                    } catch (ApplicationException | ResourceNotFoundException | ContainerStartException e) {
                        log.error(e.getMessage());
                        Platform.runLater(() -> {
                            Context.showNotificationDialog("Error starting container", e.getMessage(), Alert.AlertType.ERROR);
                        });
                        Context.HOST_STATUS_OBSERVABLE.notifyObservers("Error starting container" + e.getMessage());
                    }
                });

                Context.HOST_STATUS_OBSERVABLE.notifyObservers(String.format("Composition %s has been started", composition.getName()));
            }).start();
        }
    }


    public void stopComposition(ActionEvent actionEvent) {
        Composition composition = listAvailableCompositions.getSelectionModel().getSelectedItem();

        if (composition != null) {
            new Thread(() -> {
                Context.HOST_STATUS_OBSERVABLE.notifyObservers("Stopping composition " + composition.getName());

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
                        Context.HOST_STATUS_OBSERVABLE.notifyObservers("Stopping service " + service.getService().getName());
                        stopContainer(container.get());
                        Context.HOST_STATUS_OBSERVABLE.notifyObservers(String.format("Service %s has been stopped", service.getService().getName()));
                    } else {
                        Platform.runLater(() -> {
                            Context.showNotificationDialog("Container not found", String.format("Could not find container for service %s", service.getService().getName()), Alert.AlertType.WARNING);
                        });
                    }
                });

                Context.HOST_STATUS_OBSERVABLE.notifyObservers(String.format("Composition %s has been stopped", composition.getName()));
            }).start();
        }
    }


    private void close() {
        ((Stage)lblTitle.getScene().getWindow()).close();
    }
}
