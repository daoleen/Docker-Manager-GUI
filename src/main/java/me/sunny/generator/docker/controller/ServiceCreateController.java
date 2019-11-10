package me.sunny.generator.docker.controller;


import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.domain.*;
import me.sunny.generator.docker.enums.DockerDependCondition;
import me.sunny.generator.docker.enums.DockerRestartOption;
import org.apache.commons.lang3.StringUtils;


@Slf4j
public class ServiceCreateController {
    private final ObservableList<DockerPortMapping> ports = FXCollections.observableArrayList();
    private final ObservableList<DockerVolumeMapping> volumes = FXCollections.observableArrayList();
    private final ObservableList<DockerService> services = FXCollections.observableArrayList(
            Context.project.getAvailableServices().stream().map(DockerServiceDescription::getService).collect(Collectors.toList()));
    private final ObservableList<DockerService> links = FXCollections.observableArrayList();
    private final ObservableList<Map.Entry<String, String>> environments = FXCollections.observableArrayList();
    private final ObservableList<DockerDepend> depends = FXCollections.observableArrayList();
    private final ObservableList<DockerDependCondition> dependConditions = FXCollections.observableArrayList(DockerDependCondition.values());
    private final ObservableList<DockerRestartOption> restartOptions = FXCollections.observableArrayList(DockerRestartOption.values());


    @FXML
    private TextField txtName;

    @FXML
    private TextField txtImage;

    @FXML
    private ComboBox<DockerRestartOption> selRestart;

    @FXML
    private TableView<DockerPortMapping> tblPorts;

    @FXML
    private TableColumn<DockerPortMapping, Integer> tblPortsColHost;

    @FXML
    private TableColumn<DockerPortMapping, Integer> tblPortsColContainer;

    @FXML
    private TableView<DockerVolumeMapping> tblVolumes;

    @FXML
    private TableColumn<DockerVolumeMapping, String> tblVolumesColHost;

    @FXML
    private TableColumn<DockerVolumeMapping, String> tblVolumesColContainer;

    @FXML
    private ComboBox<DockerService> selLinks;

    @FXML
    private ListView<DockerService> listLinks;

    @FXML
    private TableView<Map.Entry<String, String>> tblEnvironments;

    @FXML
    private ListView<DockerDepend> listDepends;

    @FXML
    private ComboBox<DockerService> selDepends;

    @FXML
    private ComboBox<DockerDependCondition> selDependsCondition;

    @FXML
    private TextField txtHealthcheckCommamd;

    @FXML
    private TextField txtHealthcheckInterval;

    @FXML
    private TextField txtHealthcheckTimeout;

    @FXML
    private TextField txtHealthcheckRetries;


    @FXML
    private void initialize() {
        initRestartOptionsCombo();
        initPortsTable();
        initVolumesTable();
        initServicesCombo();
        initLinksList();
        initEnvironmentsTable();
        initDependsList();
        initDependsConditionCombo();
        initHealthcheckControls();
    }


    private void initRestartOptionsCombo() {
        selRestart.setItems(restartOptions);
        selRestart.getSelectionModel().select(DockerRestartOption.NO);
    }


    private void initVolumesTable() {
        tblVolumesColHost.setCellValueFactory(new PropertyValueFactory<>("hostVolumePath"));
        tblVolumesColContainer.setCellValueFactory(new PropertyValueFactory<>("containerVolumePath"));

        Platform.runLater(() -> tblVolumes.setItems(volumes));

        tblVolumes.setEditable(true);
        tblVolumesColHost.setEditable(true);
        tblVolumesColContainer.setEditable(true);

        tblVolumesColHost.setCellFactory(TextFieldTableCell.forTableColumn());

        tblVolumesColHost.setOnEditCommit(event -> {
            DockerVolumeMapping volumeMapping = event.getRowValue();
            volumeMapping.setHostVolumePath(event.getNewValue());
        });

        tblVolumesColContainer.setCellFactory(TextFieldTableCell.forTableColumn());

        tblVolumesColContainer.setOnEditCommit(event -> {
            DockerVolumeMapping volumeMapping = event.getRowValue();
            volumeMapping.setContainerVolumePath(event.getNewValue());
        });
    }


    private void initPortsTable() {
        tblPortsColHost.setCellValueFactory(new PropertyValueFactory<>("hostPort"));
        tblPortsColContainer.setCellValueFactory(new PropertyValueFactory<>("containerPort"));

        Platform.runLater(() -> tblPorts.setItems(ports));

        tblPorts.setEditable(true);
        tblPortsColHost.setEditable(true);
        tblPortsColContainer.setEditable(true);

        final StringConverter<Integer> integerStringConverter = new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                return object.toString();
            }


            @Override
            public Integer fromString(String string) {
                return Integer.parseInt(string);
            }
        };

        tblPortsColHost.setCellFactory(TextFieldTableCell.forTableColumn(integerStringConverter));

        tblPortsColHost.setOnEditCommit(event -> {
            DockerPortMapping portMapping = event.getRowValue();
            portMapping.setHostPort(event.getNewValue());
        });

        tblPortsColContainer.setCellFactory(TextFieldTableCell.forTableColumn(integerStringConverter));

        tblPortsColContainer.setOnEditCommit(event -> {
            DockerPortMapping portMapping = event.getRowValue();
            portMapping.setContainerPort(event.getNewValue());
        });
    }


    private void initServicesCombo() {
        selLinks.setItems(services);
        selDepends.setItems(services);
    }


    private void initLinksList() {
        listLinks.setItems(links);
    }


    private void initEnvironmentsTable() {
        TableColumn<Map.Entry<String, String>, String> tblEnvironmentsColKey = new TableColumn<>("Key");
        tblEnvironmentsColKey.setPrefWidth(199d);
        tblEnvironmentsColKey.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getKey()));

        TableColumn<Map.Entry<String, String>, String> tblEnvironmentsColValue = new TableColumn<>("Value");
        tblEnvironmentsColValue.setPrefWidth(246d);
        tblEnvironmentsColValue.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue()));

        tblEnvironments.getColumns().setAll(tblEnvironmentsColKey, tblEnvironmentsColValue);
        Platform.runLater(() -> tblEnvironments.setItems(environments));

        tblEnvironments.setEditable(true);
        tblEnvironmentsColKey.setEditable(true);
        tblEnvironmentsColValue.setEditable(true);

        tblEnvironmentsColKey.setCellFactory(TextFieldTableCell.forTableColumn());
        tblEnvironmentsColKey.setOnEditCommit(event -> {
            EnvironmentEntry environment = (EnvironmentEntry) event.getRowValue();
            environment.setKey(event.getNewValue());
        });

        tblEnvironmentsColValue.setCellFactory(TextFieldTableCell.forTableColumn());
        tblEnvironmentsColValue.setOnEditCommit(event -> {
            Map.Entry<String, String> environment = event.getRowValue();
            environment.setValue(event.getNewValue());
        });
    }


    private void initDependsList() {
        listDepends.setItems(depends);
    }


    private void initDependsConditionCombo() {
        selDependsCondition.setItems(dependConditions);
    }


    private void initHealthcheckControls() {
        txtHealthcheckInterval.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtHealthcheckInterval.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        txtHealthcheckTimeout.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtHealthcheckTimeout.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        txtHealthcheckRetries.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtHealthcheckRetries.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }


    public void addPort(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            ports.add(new DockerPortMapping());
        });
    }


    public void removePort(ActionEvent actionEvent) {
        DockerPortMapping selectedItem = tblPorts.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Platform.runLater(() -> ports.remove(selectedItem));
        }
    }


    public void addVolume(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            volumes.add(new DockerVolumeMapping());
        });
    }


    public void removeVolume(ActionEvent actionEvent) {
        DockerVolumeMapping selectedItem = tblVolumes.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Platform.runLater(() -> volumes.remove(selectedItem));
        }
    }


    public void addLink(ActionEvent actionEvent) {
        DockerService selectedItem = selLinks.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Platform.runLater(() -> links.add((DockerService) selectedItem));
        }
    }


    public void removeLink(ActionEvent actionEvent) {
        DockerService selectedItem = selLinks.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Platform.runLater(() -> links.remove(selectedItem));
        }
    }


    public void addEnvironment(ActionEvent actionEvent) {
        Platform.runLater(() -> environments.add(new EnvironmentEntry("", "")));
    }


    public void removeEnvironment(ActionEvent actionEvent) {
        Map.Entry<String, String> selectedItem = tblEnvironments.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Platform.runLater(() -> environments.remove(selectedItem));
        }
    }


    public void addDepends(ActionEvent actionEvent) {
        DockerService selectedService = selDepends.getSelectionModel().getSelectedItem();
        DockerDependCondition selectedCondition = selDependsCondition.getSelectionModel().getSelectedItem();
        if (selectedService != null && selectedCondition != null) {
            Platform.runLater(() -> depends.add(new DockerDepend(selectedService.getId(), selectedCondition)));
        }
    }


    public void removeDepends(ActionEvent actionEvent) {
        DockerDepend selectedItem = listDepends.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Platform.runLater(() -> depends.remove(selectedItem));
        }
    }


    public void save(ActionEvent actionEvent) {
        DockerRestartOption restartOption = (selRestart.getSelectionModel().getSelectedItem() != null) ?
                (DockerRestartOption) selRestart.getSelectionModel().getSelectedItem() : null;
        HashSet<DockerPortMapping> portMappings = new HashSet<>(ports);
        HashSet<DockerVolumeMapping> volumeMappings = new HashSet<>(volumes);
        Map<String, String> environmentVars = new HashMap<>(environments.size());
        environments.forEach(env -> {
            environmentVars.put(env.getKey(), env.getValue());
        });
        HashSet<DockerDepend> dependServices = new HashSet<>(depends);
        HashSet<DockerService> linkServices = new HashSet<>(links);

        DockerHealthchek healthchek = null;

        if (StringUtils.isNotEmpty(txtHealthcheckCommamd.getText())) {
            int interval = 0, timeout = 0, retries = 0;

            if (StringUtils.isNotEmpty(txtHealthcheckInterval.getText())) {
                interval = Integer.parseInt(txtHealthcheckInterval.getText());
            }

            if (StringUtils.isNotEmpty(txtHealthcheckTimeout.getText())) {
                timeout = Integer.parseInt(txtHealthcheckTimeout.getText());
            }

            if (StringUtils.isNotEmpty(txtHealthcheckRetries.getText())) {
                retries = Integer.parseInt(txtHealthcheckRetries.getText());
            }

            healthchek = new DockerHealthchek(txtHealthcheckCommamd.getText(), interval, timeout, retries);
        }

        Set<UUID> linksSet = linkServices.stream().map(DockerService::getId).collect(Collectors.toSet());

        DockerService createdService = new DockerService(UUID.randomUUID(), txtName.getText(), txtImage.getText(), "", restartOption, portMappings,
                        volumeMappings, environmentVars, dependServices, linksSet, healthchek);

        Context.project.getAvailableServices().add(new DockerServiceDescription(createdService));
        openDetailsWindow(createdService.getName());
    }


    private void openDetailsWindow(String serviceName) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("service/details.fxml"));
            Stage serviceDetailsStage = new Stage();
            serviceDetailsStage.initModality(Modality.WINDOW_MODAL);
            serviceDetailsStage.setTitle(serviceName);
            serviceDetailsStage.setScene(new Scene(fxmlLoader.load()));
            ServiceDetailsController serviceDetailsController = fxmlLoader.<ServiceDetailsController>getController();
            serviceDetailsController.init(serviceName);
            serviceDetailsStage.show();

            ((Stage) txtName.getScene().getWindow()).close();
        } catch (IOException ex) {
            log.error("Could not open window for details of service: {}", ex.getMessage());
            Context.showNotificationDialog("Error", "Could not open window for details of service", Alert.AlertType.ERROR);
        }
    }


    @AllArgsConstructor
    private final static class EnvironmentEntry implements Map.Entry<String, String> {
        private String key;
        private String value;


        @Override
        public String getKey() {
            return key;
        }


        @Override
        public String getValue() {
            return value;
        }


        @Override
        public String setValue(String value) {
            return this.value = value;
        }


        public void setKey(String key) {
            this.key = key;
        }
    }
}
