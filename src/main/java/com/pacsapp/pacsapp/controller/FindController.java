package com.pacsapp.pacsapp.controller;

import com.pacsapp.pacsapp.concurrent.FindOnPACSService;
import com.pacsapp.pacsapp.container.PACSServer;
import com.pacsapp.pacsapp.util.FXUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FindController {

    @FXML
    private Label errorLabel;
    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private TextField patientNameText;
    @FXML
    private TextField patientIdText;
    @FXML
    private TextField birthDateText;
    @FXML
    private TextField studyDateText;
    @FXML
    private ComboBox<DICOMModality> modalityBox;

    @FXML
    private ChoiceBox retrieveLevelBox;

    @FXML
    private Button cancelButton;
    @FXML
    private Button findButton;

    @Setter
    private PACSServer pacsServer;

    private String modality;

    @FXML
    private void find() {
        String patientName = patientNameText.getText();
        String patientId = patientIdText.getText();
        String birthDate = birthDateText.getText();
        String studyDate = studyDateText.getText();
        String retrieveLevel = retrieveLevelBox.getValue().toString();

        List<String> keysList=new ArrayList<>();
        if(!patientName.isEmpty()) {
            keysList.add("PatientName="+patientName+"*");
        }
        if(!patientId.isEmpty()) {
            keysList.add("PatientID="+patientId);
        }
        if(!birthDate.isEmpty()) {
            keysList.add("PatientBirthDate="+birthDate);
        }
        if(!studyDate.isEmpty()) {
            keysList.add("StudyDate="+studyDate);
        }
        if(modality!=null) {
            keysList.add("ModalitiesInStudy="+modality);
        }
        if (keysList==null||keysList.size()==0) {
            error("No keys supplied");
            return;
        }

        errorLabel.setVisible(false);
        progressIndicator.setVisible(true);
        setUiState(true);

        FindOnPACSService srv = new FindOnPACSService(pacsServer,keysList.toArray(new String[keysList.size()]), retrieveLevel);

        srv.setOnSucceeded(workerStateEvent -> {
            setUiState(false);
            progressIndicator.setVisible(false);
            errorLabel.setVisible(false);
            try {
                DownloadController downloadController = (DownloadController) FXUtils.openFXMLWindow("download", "Download from PACS", javafx.stage.Modality.WINDOW_MODAL, StageStyle.DECORATED, findButton.getScene().getWindow());
                downloadController.setPacsServer(pacsServer);
            } catch (IOException e) {
                log.error("Failed to open download window", e);
            }
        });

        srv.setOnFailed(workerStateEvent -> {
            setUiState(false);
            error(srv.getException().getCause() != null ? srv.getException().getCause().getMessage() : srv.getException().getMessage());
        });

        srv.start();
    }



    private void setUiState(boolean disabled) {
        patientNameText.setDisable(disabled);
        patientIdText.setDisable(disabled);
        birthDateText.setDisable(disabled);
        studyDateText.setDisable(disabled);
        modalityBox.setDisable(disabled);
        retrieveLevelBox.setDisable(disabled);
        findButton.setDisable(disabled);
        cancelButton.setDisable(disabled);
    }

    @FXML
    private void initialize() {

        modalityBox.getSelectionModel().selectedItemProperty().addListener((observableValue, val, val2) -> {
                if (val2!=null) {
                    modality=val2.getAbbr();
                } else {
                    modality=null;
                }
        });

        modalityBox.getItems().addAll(
                new DICOMModality("XA", "X-Ray Angiography"),
                new DICOMModality("US", "Ultrasound"),
                new DICOMModality("RF", "Radio Fluoroscopy"));

        modalityBox.setEditable(true);

        modalityBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DICOMModality m) {
                if (m != null) {
                    return m.getAbbr();
                }
                return "";
            }

            @Override
            public DICOMModality fromString(String string) {
                return new DICOMModality(string,"");
            }
        });

        modalityBox.setCellFactory(param -> new ListCell<>() {

            @Override
            public void updateItem(DICOMModality m, boolean empty) {
                super.updateItem(m, empty);

                if (m != null) {
                    setText(m.getAbbr());
                    setTooltip(new Tooltip(m.getTooltip()));
                } else {
                    setText(null);
                    setTooltip(null);
                }
            }
        });

        String[] rl = {"STUDY", "IMAGE"};
        retrieveLevelBox.setItems(FXCollections.observableArrayList(rl));
        retrieveLevelBox.setValue(retrieveLevelBox.getItems().get(0));
    }

    private class DICOMModality {
        private String abbr;
        private String tooltip;

        private String getAbbr() {
            return abbr;
        }

        private String getTooltip() {
            return tooltip;
        }

        private DICOMModality(String abbr, String tooltip) {
            this.abbr = abbr;
            this.tooltip = tooltip;
        }
    }

    @FXML
    private void cancel() {
        Stage stage = (Stage) findButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void error(String errorText){
        Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            errorLabel.setVisible(true);
            errorLabel.setText(errorText);
        });
    }

}
