package me.sunny.generator.docker.controller;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.domain.*;
import me.sunny.generator.docker.exception.ApplicationException;
import me.sunny.generator.docker.exception.ResourceNotFoundException;
import me.sunny.generator.docker.util.DockerComposeWriter;


@Slf4j
public class CompositionController {
    private final ObservableList<DockerServiceConcreted> selectedConcretedServices = FXCollections.observableList(new ArrayList<>());
    private final Composition emptyComposition = new Composition("--- New Composition ---", null);
    private ObservableList<Composition> compositions;


    @FXML
    private ComboBox<Composition> selComposition;

    @FXML
    private ListView<DockerService> listAvailableServices;

    @FXML
    private TableView<DockerServiceConcreted> tblSelectedServices;

    @FXML
    private TableColumn<DockerServiceConcreted, String> tblSelectedServicesColService;

    @FXML
    private TableColumn<DockerServiceConcreted, String> tblSelectedServicesColVersion;


    @FXML
    public void initialize() {
        initSelCompositions();
        initTblSelectedServices();
        initAvailableServices();
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

        Callback<TableColumn<DockerServiceConcreted, String>, TableCell<DockerServiceConcreted, String>> cellVersionCallback = new Callback<TableColumn<DockerServiceConcreted, String>, TableCell<DockerServiceConcreted, String>>() {
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

        tblSelectedServices.setItems(selectedConcretedServices);
        initTblSelectedServicesData();
    }


    private void initSelCompositions() {
        ArrayList<Composition> compositions = new ArrayList<>(Context.project.getCompositions().size() + 1);
        compositions.add(emptyComposition);
        compositions.addAll(Context.project.getCompositions());
        this.compositions = FXCollections.observableList(compositions);

        selComposition.setItems(this.compositions);
        selComposition.getSelectionModel().select(emptyComposition);
    }


    private void initAvailableServices() {
        ObservableList<DockerService> availableServices = loadAvailableServices();
        listAvailableServices.setItems(availableServices);
    }


    private ObservableList<DockerService> loadAvailableServices() {
        return FXCollections.observableList(
                Context.project.getAvailableServices().stream()
                        .map(DockerServiceDescription::getService)
                        .filter(availableService -> {
                            // exclude services from selected table
                            return tblSelectedServices.getItems().stream()
                                    .noneMatch(concreteService -> concreteService.getServiceId().equals(availableService.getId()));
                        })
                        .collect(Collectors.toList()));
    }


    public void addConcretedService(ActionEvent actionEvent) {
        DockerService selectedService = listAvailableServices.getSelectionModel().getSelectedItem();

        if (selectedService != null) {
            if (selectedService.getDepends() != null && (!selectedService.getDepends().isEmpty())) {
                selectedService.getDepends().forEach(depend -> {
                    if (selectedConcretedServices.stream().noneMatch(cs -> cs.getServiceId().equals(depend.getServiceId()))) {
                        addDockerServiceToConcreted(depend.getServiceId());
                    }
                });
            }

            addDockerServiceToConcreted(selectedService.getId());
        }

        // reinit available services list
        initAvailableServices();
    }


    private void addDockerServiceToConcreted(UUID serviceId) {
        List<String> versions = null;
        try {
            versions = Context.project.findService(serviceId).getVersions();
        } catch (ResourceNotFoundException e) {
            log.error(e.getMessage(), e);
            versions = Collections.emptyList();
        }

        DockerServiceConcreted concretedService = new DockerServiceConcreted(serviceId, (versions.isEmpty() ? null : versions.get(versions.size() - 1)));

        if (!selectedConcretedServices.contains(concretedService)) {
            selectedConcretedServices.add(concretedService);
        }
    }


    public void removeConcretedService(ActionEvent actionEvent) {
        DockerServiceConcreted selectedServiceConcreted = tblSelectedServices.getSelectionModel().getSelectedItem();

        if (selectedServiceConcreted != null) {
            tblSelectedServices.getItems().remove(selectedServiceConcreted);
        }

        // reinit available services list
        initAvailableServices();
    }


    public void applyComposition(ActionEvent actionEvent) {
        Composition composition = selComposition.getSelectionModel().getSelectedItem();

        if (composition != null) {
            if (emptyComposition.equals(composition)) {
                log.warn("Empty composition");
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("dialog.fxml"));
                Stage dialogStage = new Stage();
                dialogStage.initModality(Modality.APPLICATION_MODAL);
                dialogStage.setTitle("Enter new composition name");
                try {
                    dialogStage.setScene(new Scene(fxmlLoader.load()));
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    Context.showNotificationDialog("Error applying composition", e.getMessage(), Alert.AlertType.ERROR);
                }
                DialogController dialogController = fxmlLoader.<DialogController>getController();
                dialogController.init("Please enter new composition name here", "");
                dialogStage.showAndWait();

                log.debug("After wait: {}", dialogController.getContent());

                if (dialogController.isCancelled()) {
                    log.info("Skip applying new composition because it was cancelled by user");
                    return;
                }

                composition = new Composition(dialogController.getContent(), null);
                Context.project.getCompositions().add(composition);
            }

            ObservableList<DockerServiceConcreted> selectedConcretedServices = tblSelectedServices.getItems();
            composition.setServices(new HashSet<>(selectedConcretedServices));
            initSelCompositions();
            selComposition.getSelectionModel().select(composition);
        }
    }


    private void initTblSelectedServicesData() {
        Composition composition = selComposition.getSelectionModel().getSelectedItem();
        selectedConcretedServices.clear();

        if (composition != null && !emptyComposition.equals(composition)) {
            // add all concreted services (services with specified version)
            selectedConcretedServices.addAll(composition.getServices());

            composition.getServices().stream()
                    .flatMap(serviceConcreted -> {
                        DockerService service = null;
                        try {
                            service = Context.project.findService(serviceConcreted.getServiceId()).getService();
                        } catch (ResourceNotFoundException e) {
                            log.error(e.getMessage());
                            return Stream.empty();
                        }

                        if (service.getDepends() != null && !service.getDepends().isEmpty()) {
                            return service.getDepends().stream();
                        }
                        return Stream.empty();
                    })
                    .map(DockerDepend::getServiceId)
                    .distinct()
                    .forEach(srvId -> {
                        if (selectedConcretedServices.stream().map(DockerServiceConcreted::getServiceId).noneMatch(selectedServiceId -> selectedServiceId.equals(srvId))) {
                            addDockerServiceToConcreted(srvId);
                        }
                    });
        }

        initAvailableServices();
    }


    public void compositionSelected() {
        initTblSelectedServicesData();
    }


    public void close(ActionEvent actionEvent) {
        ((Stage) tblSelectedServices.getScene().getWindow()).close();
    }


    public void selectComposition(Composition composition) {
        selComposition.getSelectionModel().select(composition);
        compositionSelected();
    }


    public void generateDockerCompose(ActionEvent actionEvent) {
        if (selComposition.getSelectionModel().getSelectedItem() != null) {
            Composition composition = selComposition.getSelectionModel().getSelectedItem();

            if (emptyComposition.equals(composition)) {
                Context.showNotificationDialog("Action not allowed", "You should apply composition to project first", Alert.AlertType.WARNING);
                return;
            }

            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("YAML file (*.yml)", "*.yml");
            fileChooser.getExtensionFilters().add(extensionFilter);
            fileChooser.setInitialFileName("docker-compose.yml");

            File selectedFile = fileChooser.showSaveDialog(listAvailableServices.getScene().getWindow());

            if (selectedFile != null) {
                try {
                    DockerComposeWriter.generateCompose(composition, selectedFile);
                    Context.showNotificationDialog("File saved", "File was successfully generated", Alert.AlertType.INFORMATION);
                } catch (ApplicationException e) {
                    Context.showNotificationDialog("Error generating compose", e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        }
    }
}
