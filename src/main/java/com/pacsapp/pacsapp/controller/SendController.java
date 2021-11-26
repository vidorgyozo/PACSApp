package com.pacsapp.pacsapp.controller;

import com.pacsapp.pacsapp.concurrent.SendToPACSService;
import com.pacsapp.pacsapp.container.PACSServer;
import com.pacsapp.pacsapp.util.FXUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;

import java.io.File;


public class SendController {

    @Setter
    private PACSServer pacsServer;

    @FXML
    private TextField fileField;
    @FXML
    private Button browseButton;
    @FXML
    private Button sendButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label rspLabel;
    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private void browse(){
        FileChooser fileChooser = FXUtils.getDicomFileChooser("Send file to PACS");
        File file = fileChooser.showOpenDialog(fileField.getScene().getWindow());

        if(file != null){
            fileField.setText(file.getAbsolutePath());
        }

    }

    @FXML
    private void send(){
        File file = new File(fileField.getText());
        if(!file.exists()){
            response("Select an existing file");
            return;
        }

        rspLabel.setVisible(false);
        progressIndicator.setVisible(true);
        setUiState(true);

        SendToPACSService srv = new SendToPACSService(pacsServer, file, this);

        srv.setOnSucceeded(workerStateEvent -> {
            setUiState(false);
            progressIndicator.setVisible(false);
        });

        srv.setOnFailed(workerStateEvent -> {
            response("Failed to send file to PACS: " + (srv.getException().getCause() != null ?
                    srv.getException().getCause().getMessage() : srv.getException().getMessage()));
        });

        srv.start();
    }

    @FXML
    private void cancel(){
        ((Stage)fileField.getScene().getWindow()).close();
    }

    public void response(String responseString){
        Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            rspLabel.setVisible(true);
            rspLabel.setText(responseString);
            setUiState(false);
        });
    }

    private void setUiState(boolean disabled) {
        browseButton.setDisable(disabled);
        fileField.setDisable(disabled);
        sendButton.setDisable(disabled);
        cancelButton.setDisable(disabled);
    }
}
