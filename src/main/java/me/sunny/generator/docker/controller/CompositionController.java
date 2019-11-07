package me.sunny.generator.docker.controller;


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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.domain.*;
import me.sunny.generator.docker.exception.ResourceNotFoundException;


@Slf4j
public class CompositionController {
    private final ObservableList<DockerServiceConcreted> selectedConcretedServices = FXCollections.observableArrayList();
    private final Composition emptyComposition = new Composition("--- New Composition ---", null);
    private ObservableList<Composition> compositions;


    @FXML
    private ComboBox<Composition> selComposition;

    @FXML
    private ListView listAvailableServices;

    @FXML
    private TableView tblSelectedServices;

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
        tblSelectedServicesColService.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getService().getName()));
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
                            String selectedService = dockerServiceConcretedSelected.getService().getName();
                            List<String> versions = null;
                            try {
                                versions = Context.project.findService(selectedService).getVersions();
                            } catch (ResourceNotFoundException e) {
                                log.error(e.getMessage(), e);
                                versions = Collections.emptyList();
                            }

                            comboBox.setItems(FXCollections.observableArrayList(versions));
                            comboBox.setOnAction(event -> {
                                dockerServiceConcretedSelected.setVersion(comboBox.getSelectionModel().getSelectedItem());
                            });

                            tblSelectedServices.getItems()
                                    .stream()
                                    .filter(s -> ((DockerServiceConcreted) s).getService().getName()
                                            .equals(selectedService))
                                    .findFirst()
                                    .ifPresent(serviceConcreted -> {
                                        String version = ((DockerServiceConcreted) serviceConcreted).getVersion();
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
        this.compositions = FXCollections.observableArrayList(compositions);

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
                                    .noneMatch(concreteService -> ((DockerServiceConcreted)concreteService).getService().equals(availableService));
                        })
                        .collect(Collectors.toList()));
    }


    public void addConcretedService(ActionEvent actionEvent) {
        Object selectedItem = listAvailableServices.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            DockerService selectedService = (DockerService) selectedItem;

            if (selectedService.getDepends() != null && (!selectedService.getDepends().isEmpty())) {
                selectedService.getDepends().forEach(depend -> {
                    if (selectedConcretedServices.stream().noneMatch(cs -> ((DockerServiceConcreted) cs).getService().equals(depend.getService()))) {
                        addDockerServiceToConcreted(depend.getService());
                    }
                });
            }

            addDockerServiceToConcreted(selectedService);
        }

        // reinit available services list
        initAvailableServices();
    }


    private void addDockerServiceToConcreted(DockerService selectedService) {
        List<String> versions = null;
        try {
            versions = Context.project.findService(selectedService.getName()).getVersions();
        } catch (ResourceNotFoundException e) {
            log.error(e.getMessage(), e);
            versions = Collections.emptyList();
        }

        DockerServiceConcreted concretedService = new DockerServiceConcreted(selectedService, (versions.isEmpty() ? null : versions.get(versions.size() - 1)));

        if (!selectedConcretedServices.contains(concretedService)) {
            selectedConcretedServices.add(concretedService);
        }
    }


    public void removeConcretedService(ActionEvent actionEvent) {
        Object selectedItem = tblSelectedServices.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            DockerServiceConcreted selectedServiceConcreted = (DockerServiceConcreted) selectedItem;

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
        Object selectedComposition = selComposition.getSelectionModel().getSelectedItem();
        selectedConcretedServices.clear();

        if (selectedComposition != null && !emptyComposition.equals(selectedComposition)) {
            // load composition services
            Composition composition = (Composition) selectedComposition;

            // add all concreted services (services with specified version)
            selectedConcretedServices.addAll(composition.getServices());

            composition.getServices().stream()
                    .flatMap(serviceConcreted -> {
                        if (serviceConcreted.getService().getDepends() != null && !serviceConcreted.getService().getDepends().isEmpty()) {
                            return serviceConcreted.getService().getDepends().stream();
                        }
                        return Stream.empty();
                    })
                    .map(DockerDepend::getService)
                    .distinct()
                    .forEach(srv -> {
                        if (selectedConcretedServices.stream().map(DockerServiceConcreted::getService).noneMatch(selectedService -> selectedService.getName().equals(srv.getName()))) {
                            addDockerServiceToConcreted(srv);
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
}
