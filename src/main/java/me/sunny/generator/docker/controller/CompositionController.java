package me.sunny.generator.docker.controller;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
import me.sunny.generator.docker.domain.DockerService;
import me.sunny.generator.docker.domain.DockerServiceConcreted;
import me.sunny.generator.docker.domain.DockerServiceDescription;
import me.sunny.generator.docker.exception.ResourceNotFoundException;


@Slf4j
public class CompositionController {

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
        initAvailableServices();

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
                    if (tblSelectedServices.getItems().stream().noneMatch(cs -> ((DockerServiceConcreted) cs).getService().equals(depend.getService()))) {
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

        if (!tblSelectedServices.getItems().contains(concretedService)) {
            tblSelectedServices.getItems().add(concretedService);
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
}
