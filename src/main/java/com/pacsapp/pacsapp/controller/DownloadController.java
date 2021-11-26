package com.pacsapp.pacsapp.controller;

import com.pacsapp.pacsapp.concurrent.DownloadFromPACSService;
import com.pacsapp.pacsapp.container.PACSServer;
import com.pacsapp.pacsapp.treetable.TreeDataProperty;
import com.pacsapp.pacsapp.treetable.TreeTablePopulator;
import com.pacsapp.pacsapp.util.FileUtilities;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.util.SafeClose;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DownloadController {
    @FXML
    private GridPane gridPane;
    @FXML
    private TreeTableView<TreeDataProperty> pacsDownloadPropertiesTable;
    @FXML
    private Label rspLabel;
    @FXML
    private Button backButton;
    @FXML
    private Button downloadButton;
    @FXML
    private ProgressIndicator progressIndicator;

    private TreeTablePopulator treeTablePopulator = new TreeTablePopulator();

    private static final int itemsPerPage = 5;

    @Setter
    private PACSServer pacsServer;

    private String tagRetrieveLevelUID;
    private String retrieveLevel;
    private String tagName;

    @FXML
    private void initialize() throws IOException {
        File outDir=new File(FilenameUtils.concat(FileUtilities.getPACSAppUserFolder().getAbsolutePath(),"tmp/query/"));
        File[] directoryListing = outDir.listFiles();
        if (directoryListing != null) {
            ArrayList<Attributes> resultList = new ArrayList<>();
            for (File file : directoryListing) {
                DicomInputStream dis = new DicomInputStream(file);
                Attributes attrs = new Attributes();
                try {
                    attrs.addAll(dis.readDataset(-1, -1));
                    resultList.add(attrs);
                } finally {
                    SafeClose.close(dis);
                }
            }

            Pagination pagination = new Pagination();
            pagination.setPageCount((resultList.size() - 1) / itemsPerPage + 1);
            pagination.setCurrentPageIndex(0);
            pagination.setMaxPageIndicatorCount(3);

            pacsDownloadPropertiesTable= new TreeTableView<>();
            pacsDownloadPropertiesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            pagination.setPageFactory((pageIndex) -> {

                ArrayList<String> uidList=new ArrayList<>();
                ArrayList<Attributes> pageResultList = new ArrayList<>();
                int page = pageIndex * itemsPerPage;
                for (int i = page; i < page + itemsPerPage && i < resultList.size(); i++) {
                    Attributes attrs = resultList.get(i);

                    log.info(attrs.toString());

                    retrieveLevel = attrs.getString(Tag.QueryRetrieveLevel);
                    if(retrieveLevel.equals("IMAGE")){
                        tagRetrieveLevelUID = attrs.getString(Tag.SOPInstanceUID);
                        tagName = "SOPInstanceUID";
                    }
                    else{
                        tagRetrieveLevelUID = attrs.getString(Tag.StudyInstanceUID);
                        tagName = "StudyInstanceUID";
                    }
                    if(attrs.getString(Tag.StudyInstanceUID) != null && attrs.getString(Tag.StudyInstanceUID).length() != 0) {
                        uidList.add(tagRetrieveLevelUID);
                    } else {
                        String string = "";
                        if(attrs.getString(Tag.PatientName) != null) {
                            string+=attrs.getString(Tag.PatientName);
                        }
                        if(attrs.getString(Tag.PatientBirthDate) != null) {
                            string+=", "+attrs.getString(Tag.PatientBirthDate);
                        }
                        if(attrs.getString(Tag.PatientID) != null) {
                            string+=", "+attrs.getString(Tag.PatientID);
                        }
                        if(attrs.getString(Tag.StudyDate) != null) {
                            string+=", "+attrs.getString(Tag.StudyDate);
                        }
                        log.info("SOP(Study)InstanceUID unavailable, using "+string);
                        uidList.add(string);
                    }
                    pageResultList.add(attrs);
                }

                treeTablePopulator.createColumns(pacsDownloadPropertiesTable, "Property", uidList);
                treeTablePopulator.clearTable(pacsDownloadPropertiesTable);
                treeTablePopulator.populatePacsDownloadData(pacsDownloadPropertiesTable, pageResultList);

                return pacsDownloadPropertiesTable;

            });
            GridPane.setConstraints(pagination, 0, 1);
            GridPane.setColumnSpan(pagination, 2);
            GridPane.setHalignment(pagination, HPos.LEFT);
            gridPane.getChildren().addAll(pagination);

            if(resultList.size() == 0) {
                downloadButton.setDisable(true);
                rspLabel.setText("In case you cannot find the DICOM series you were looking for, "
                        + "please repeat your query using the PACS system's own client.");
            } else {
                downloadButton.setDisable(false);
                rspLabel.setText("");
            }
        }
    }

    @FXML
    private void handleDownloadAction() {
        setUiState(true);
        rspLabel.setVisible(false);
        progressIndicator.setVisible(true);
        List<String> keysList=new ArrayList<>();
        ObservableList<Integer> selected = pacsDownloadPropertiesTable.getSelectionModel().getSelectedIndices();
        ObservableList subroots = pacsDownloadPropertiesTable.getRoot().getChildren();
        for (Object child1 : selected) {
            if (child1 instanceof Integer) {
                Integer selection = (Integer) child1;
                for (Object child2 : subroots) {
                    if (child2 instanceof TreeItem) {
                        TreeItem subroot = (TreeItem) child2;
                        if(subroot == pacsDownloadPropertiesTable.getTreeItem(selection)) {
                            TreeDataProperty value = pacsDownloadPropertiesTable.getTreeItem(selection).getValue();
                            keysList.add(tagName);
                            keysList.add(value.getName());
                            log.info("SOP(Study)InstanceUID: "+value.getName());
                            break;
                        } else if(subroot == pacsDownloadPropertiesTable.getTreeItem(selection).getParent()) {
                            TreeDataProperty value = pacsDownloadPropertiesTable.getTreeItem(selection).getParent().getValue();
                            keysList.add(tagName);
                            keysList.add(value.getName());
                            log.info("SOP(Study)InstanceUID: "+value.getName());
                            break;
                        }
                    }
                }
            }
        }
        DownloadFromPACSService srv = new DownloadFromPACSService(pacsServer, keysList.toArray(new String[keysList.size()]), retrieveLevel, this);

        srv.setOnSucceeded(workerStateEvent -> setUiState(false));

        srv.setOnFailed(t -> {
            log.error("Download from PACS failed");
            response("Download from PACS failed: " + (srv.getException().getCause() != null ?
                    srv.getException().getCause().getMessage() : srv.getException().getMessage()));
        });

        srv.start();
    }

    @FXML
    private void handleToFindAction() {
        ((Stage)downloadButton.getScene().getWindow()).close();
    }

    private void setUiState(boolean disabled) {
        downloadButton.setDisable(disabled);
        backButton.setDisable(disabled);
    }

    public void response(String responseText){
        Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            rspLabel.setVisible(true);
            rspLabel.setText(responseText);
            setUiState(false);
        });
    }
}
