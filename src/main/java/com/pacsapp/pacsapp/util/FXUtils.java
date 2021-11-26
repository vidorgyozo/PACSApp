package com.pacsapp.pacsapp.util;

import com.pacsapp.pacsapp.controller.PACSApplication;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class FXUtils {

    public static Object openFXMLWindow(String fxmlString, String title, Modality modality, StageStyle stageStyle, Window ownerWindow) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PACSApplication.class.getResource("../fxml/" + fxmlString+ ".fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = new Stage();
        stage.initModality(modality);
        stage.initStyle(stageStyle);
        stage.setResizable(false);
        stage.setTitle(title);
        stage.initOwner(ownerWindow);
        stage.setScene(scene);
        stage.show();

        return fxmlLoader.getController();
    }

    public static FileChooser getDicomFileChooser(String title){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        ObservableList<FileChooser.ExtensionFilter> extensionFilters = fileChooser.getExtensionFilters();
        extensionFilters.add(new FileChooser.ExtensionFilter("DICOM (*.IMA;*.dcm)", "*.IMA", "*.dcm"));
        extensionFilters.add(new FileChooser.ExtensionFilter("All files (*.*)", "*.*"));

        return fileChooser;
    }

}
