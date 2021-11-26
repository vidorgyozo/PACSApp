package com.pacsapp.pacsapp.controller;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.pacsapp.pacsapp.container.PACSServer;
import com.pacsapp.pacsapp.util.FXUtils;
import com.pacsapp.pacsapp.util.FileUtilities;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class MainController {

    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private TextField aeTitleField;

    @FXML
    private Button sendToPACSButton;

    private PACSServer pacsServer;

    @FXML
    private void initialize(){
        loadPACSServer();
    }

    @FXML
    private void openSendWindow(){
        try {
            savePACSServer();
            SendController controller = (SendController) FXUtils.openFXMLWindow("send", "Send to PACS",
                    Modality.WINDOW_MODAL, StageStyle.DECORATED, sendToPACSButton.getScene().getWindow());
            controller.setPacsServer(pacsServer);
        } catch (IOException e) {
            log.error("Failed to open send window", e);
        }
    }

    @FXML
    private void openFindWindow(){
        try {
            savePACSServer();
            FindController controller = (FindController) FXUtils.openFXMLWindow("find", "Find on PACS",
                    Modality.WINDOW_MODAL, StageStyle.DECORATED, sendToPACSButton.getScene().getWindow());
            controller.setPacsServer(pacsServer);
        } catch (IOException e) {
            log.error("Failed to open find window", e);
        }
    }

    private void savePACSServer(){
        pacsServer = new PACSServer(aeTitleField.getText(), hostField.getText(),
                Integer.parseInt(portField.getText()));
        Gson gson = new Gson();

        try  {
            FileWriter pacsServerFileWriter = new FileWriter(FileUtilities.getPACSServerDataPath());
            gson.toJson(pacsServer, pacsServerFileWriter);
            pacsServerFileWriter.flush();
            pacsServerFileWriter.close();
            log.debug("Successfully saved PACS Server data into JSON");
        } catch (IOException e) {
            log.error("Failed to write PACS Server data to JSON", e);
        }
    }

    private void loadPACSServer(){
        Gson gson = new Gson();
        try {
            JsonReader jsonReader = new JsonReader(new FileReader(FileUtilities.getPACSServerDataPath()));
            pacsServer = gson.fromJson(jsonReader, PACSServer.class);

            hostField.setText(pacsServer.getHost());
            portField.setText(String.format("%d",pacsServer.getPort()));
            aeTitleField.setText(pacsServer.getAeTitle());
            log.debug("Successfully read PACS Server data from JSON");
        } catch (FileNotFoundException e) {
            log.error("Failed to read PACS Server data from JSON file", e);
        }
    }

    @FXML
    private void exit(){
        savePACSServer();
        Platform.exit();
    }
}