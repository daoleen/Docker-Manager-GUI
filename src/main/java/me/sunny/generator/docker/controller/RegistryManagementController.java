package me.sunny.generator.docker.controller;


import java.util.List;


import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import me.sunny.generator.docker.exception.ApplicationException;
import me.sunny.generator.docker.service.DockerRegistryService;
import me.sunny.generator.docker.util.ServiceFactory;
import org.apache.commons.lang3.StringUtils;


// TODO: remove default values
@Slf4j
public class RegistryManagementController {
    private final DockerRegistryService dockerRegistryService;

    @FXML
    private TextField txtRegistryHost;

    @FXML
    private TextField txtRegistryUsername;

    @FXML
    private TextField txtRegistryPassword;

    @FXML
    private ComboBox<String> chkAvailableServices;

    @FXML
    private TableView<String> tblImageTags;

    @FXML
    private TableColumn<String, String> tblImageTagsColVersion;

    @FXML
    private TableColumn<String, String> tblImageTagsColAction;


    public RegistryManagementController() {
        dockerRegistryService = ServiceFactory.getDockerRegistryService();
    }


    public void loadServices(ActionEvent actionEvent) {
        String registryHost = txtRegistryHost.getText();
        String registryUsername = txtRegistryUsername.getText();
        String registryPassword = txtRegistryPassword.getText();

        try {
            List<String> allImages = dockerRegistryService.getAllImages(registryHost, registryUsername, registryPassword);
            chkAvailableServices.setItems(FXCollections.observableArrayList(allImages));
        } catch (ApplicationException e) {
            log.error(e.getMessage());
        }
    }


    public void onImageSelected(ActionEvent actionEvent) {
        String registryHost = txtRegistryHost.getText();
        String registryUsername = txtRegistryUsername.getText();
        String registryPassword = txtRegistryPassword.getText();
        String selectedImage =  chkAvailableServices.getSelectionModel().getSelectedItem();

        if (StringUtils.isNotEmpty(selectedImage)) {
            try {
                List<String> tags = dockerRegistryService.getImageTags(registryHost, registryUsername, registryPassword,
                        selectedImage);
                initTblImageTags(tags);
            } catch (ApplicationException e) {
                log.error(e.getMessage());
            }
        }
    }


    private void initTblImageTags(List<String> tags) {
        tblImageTags.setEditable(true); // TODO: может не надо?
        tblImageTagsColVersion.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue());
        });
        tblImageTagsColAction.setCellValueFactory(cellData -> new SimpleStringProperty("action"));

        Callback<TableColumn<String, String>, TableCell<String, String>> cellActionCallback = new Callback<TableColumn<String, String>, TableCell<String, String>>() {
            @Override
            public TableCell<String, String> call(TableColumn<String, String> param) {
                return new TableCell<String, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty) {
                            setGraphic(null);
                        } else {
                            Button btnDelete = new Button("Delete");
                            String tag = getTableView().getItems().get(getIndex());

                            btnDelete.setOnAction(event -> onBtnDelete(tag));
                            setGraphic(btnDelete);
                        }
                    }
                };
            }
        };

        tblImageTagsColAction.setCellFactory(cellActionCallback);
        tblImageTags.setItems(FXCollections.observableArrayList(tags));
    }


    private void onBtnDelete(String tag) {
        Platform.runLater(() -> {
            if (StringUtils.isNotEmpty(tag)) {
                String registryHost = txtRegistryHost.getText();
                String registryUsername = txtRegistryUsername.getText();
                String registryPassword = txtRegistryPassword.getText();
                String selectedImage = chkAvailableServices.getSelectionModel().getSelectedItem();

                try {
                    dockerRegistryService.deleteImageTag(registryHost, registryUsername, registryPassword, selectedImage, tag);
                } catch (ApplicationException e) {
                    log.error("Error deleting image: " + e.getMessage());
                }
            }

            onImageSelected(null);
        });
    }
}
