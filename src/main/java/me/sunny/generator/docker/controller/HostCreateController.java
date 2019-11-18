package me.sunny.generator.docker.controller;


import java.io.File;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import me.sunny.generator.docker.Context;
import me.sunny.generator.docker.domain.Host;
import org.apache.commons.lang3.StringUtils;


public class HostCreateController {

    @FXML
    private TextField txtHostAddress;

    @FXML
    private CheckBox chkSSL;

    @FXML
    private Label lblCertificatesPath;

    @FXML
    private TextField txtCertificatesPath;


    @FXML
    private void initialize() {
        setVisibilityCertificatesPath();
    }


    public void save(ActionEvent actionEvent) {
        Host host = new Host(txtHostAddress.getText(), chkSSL.isSelected(), txtCertificatesPath.getText());
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


    private void setVisibilityCertificatesPath() {
        boolean visible = chkSSL.isSelected();
        txtCertificatesPath.setVisible(visible);
        lblCertificatesPath.setVisible(visible);
    }
}
