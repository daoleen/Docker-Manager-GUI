package me.sunny.generator.docker.controller;


import java.io.File;
import java.util.HashSet;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.domain.Host;
import me.sunny.generator.docker.domain.HostVariable;
import org.apache.commons.lang3.StringUtils;


public class HostCreateController {
    private final ObservableList<HostVariable> hostVariables = FXCollections.observableArrayList();

    @FXML
    private TextField txtHostAddress;

    @FXML
    private CheckBox chkSSL;

    @FXML
    private Label lblCertificatesPath;

    @FXML
    private TextField txtCertificatesPath;

    @FXML
    private TableView<HostVariable> tblHostVariables;

    @FXML
    private TableColumn<HostVariable, String> tblHostVariablesColVar;

    @FXML
    private TableColumn<HostVariable, String> tblHostVariablesColVal;


    @FXML
    private void initialize() {
        setVisibilityCertificatesPath();
        initHostVariablesTable();
    }


    private void initHostVariablesTable() {
        tblHostVariablesColVar.setCellValueFactory(new PropertyValueFactory<>("variable"));
        tblHostVariablesColVal.setCellValueFactory(new PropertyValueFactory<>("value"));

        Platform.runLater(() -> tblHostVariables.setItems(hostVariables));

        tblHostVariables.setEditable(true);
        tblHostVariablesColVar.setEditable(true);
        tblHostVariablesColVal.setEditable(true);

        tblHostVariablesColVar.setCellFactory(TextFieldTableCell.forTableColumn());

        tblHostVariablesColVar.setOnEditCommit(event -> {
            HostVariable hostVariable = event.getRowValue();
            hostVariable.setVariable(event.getNewValue());
        });

        tblHostVariablesColVal.setCellFactory(TextFieldTableCell.forTableColumn());

        tblHostVariablesColVal.setOnEditCommit(event -> {
            HostVariable hostVariable = event.getRowValue();
            hostVariable.setValue(event.getNewValue());
        });
    }


    public void save(ActionEvent actionEvent) {
        HashSet<HostVariable> hostVariables = new HashSet<>(this.hostVariables);
        Host host = new Host(txtHostAddress.getText(), chkSSL.isSelected(), txtCertificatesPath.getText(), hostVariables);
        Context.project.getHosts().add(host);
        close();
    }


    public void cancel(ActionEvent actionEvent) {
        close();
    }


    private String chooseCertificatesDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select certificates directory");
        File selectedFolder = chooser.showDialog(txtCertificatesPath.getScene().getWindow());

        if (selectedFolder != null) {
            return selectedFolder.getAbsolutePath();
        }

        return null;
    }


    private void close() {
        ((Stage) txtHostAddress.getScene().getWindow()).close();
    }


    public void checkSecure(ActionEvent actionEvent) {
        if (chkSSL.isSelected()) {
            String selectedDirectory = chooseCertificatesDirectory();

            if (StringUtils.isBlank(selectedDirectory)) {
                chkSSL.setSelected(false);
            } else {
                txtCertificatesPath.setText(selectedDirectory);
            }
        }

        setVisibilityCertificatesPath();
    }


    public void addVariable(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            hostVariables.add(new HostVariable());
        });
    }


    public void removeVariable(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            HostVariable selectedItem = tblHostVariables.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                Platform.runLater(() -> hostVariables.remove(selectedItem));
            }
        });
    }


    private void setVisibilityCertificatesPath() {
        boolean visible = chkSSL.isSelected();
        txtCertificatesPath.setVisible(visible);
        lblCertificatesPath.setVisible(visible);
    }
}
