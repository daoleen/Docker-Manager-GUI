package me.sunny.generator.docker.controller;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


public class DialogController {
    private boolean cancelled = true;
    private StringProperty title = new SimpleStringProperty();
    private StringProperty content = new SimpleStringProperty();


    @FXML
    private Label lblTitle;

    @FXML
    private TextField txtContent;


    @FXML
    public void initialize() {
        lblTitle.textProperty().bind(title);
        txtContent.textProperty().bindBidirectional(content);
    }


    public void init(String title, String content) {
        this.title.setValue(title);
        this.content.setValue(content);
    }


    private void close(boolean cancelled) {
        this.cancelled = cancelled;
        ((Stage) lblTitle.getScene().getWindow()).close();
    }


    public void apply(ActionEvent actionEvent) {
        close(false);
    }


    public void cancel(ActionEvent actionEvent) {
        close(true);
    }


    public String getContent() {
        return this.content.getValue();
    }


    public boolean isCancelled() {
        return cancelled;
    }
}
