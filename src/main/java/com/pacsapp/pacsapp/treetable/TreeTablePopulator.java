package com.pacsapp.pacsapp.treetable;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TreeTablePopulator {

    private TreeDataPropertyFactory factory=new TreeDataPropertyFactory();

    public void clearTable(TreeTableView table) {
        ObservableList subroots = table.getRoot().getChildren();
        for (Object child:subroots) {
            if (child instanceof TreeItem) {
                TreeItem item=(TreeItem) child;
                item.getChildren().clear();
            }
        }
    }

    public void createColumns(TreeTableView table,String col1,List<String> subroots) {
        final TreeItem<TreeDataProperty> root =
                new TreeItem<>(new TreeDataProperty("Root", ""));
        table.setShowRoot(false);
        table.setRoot(root);

        for (String subrootName:subroots) {
            final TreeItem<TreeDataProperty> subroot =
                    new TreeItem<>(new TreeDataProperty(subrootName, ""));
            table.getRoot().getChildren().add(subroot);
            subroot.setExpanded(true);
        }


        TreeTableColumn<TreeDataProperty, String> nameCol =
                new TreeTableColumn<>(col1);
        nameCol.setPrefWidth(150);
        nameCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<TreeDataProperty, String> param) ->
                        new ReadOnlyStringWrapper(param.getValue().getValue().getName())
        );

        TreeTableColumn<TreeDataProperty, String> valueCol =
                new TreeTableColumn<>("Value");
        valueCol.setPrefWidth(350);
        valueCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<TreeDataProperty, String> param) ->
                        new ReadOnlyStringWrapper(param.getValue().getValue().getValue())
        );

        table.getColumns().setAll(nameCol, valueCol);

    }

    private TreeItem findSubRootByName(TreeTableView table,String name) {
        for (Object child:table.getRoot().getChildren()) {
            if (child instanceof TreeItem) {
                TreeItem item=(TreeItem) child;
                if (item.getValue() instanceof TreeDataProperty) {
                    TreeDataProperty prop=(TreeDataProperty) item.getValue();
                    log.debug(prop.toString()+" "+prop.getName()+" "+name);
                    if (prop.getName().equals(name)) {
                        return item;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Invalid sub root name: "+name);
    }

    public void populatePacsDownloadData(TreeTableView table, ArrayList<Attributes> pageResultList) {
        for(Attributes attrs: pageResultList) {
            List<TreeDataProperty> props=factory.createPacsDownloadTreeData(attrs);
            props.stream().forEach((prop) -> {
                String retrieveLevel = attrs.getString(Tag.QueryRetrieveLevel);
                String tagRetrieveLevelUID = null;
                if(retrieveLevel.equals("IMAGE")){
                    tagRetrieveLevelUID = attrs.getString(Tag.SOPInstanceUID);
                }
                else{
                    tagRetrieveLevelUID = attrs.getString(Tag.StudyInstanceUID);
                }
                log.debug("SOP(Study)InstanceUID: "+tagRetrieveLevelUID);
                if(tagRetrieveLevelUID!=null && tagRetrieveLevelUID.length()!=0) {
                    findSubRootByName(table, tagRetrieveLevelUID).getChildren().add(new TreeItem<>(prop));
                } else {
                    String string="";
                    if(attrs.getString(Tag.PatientName)!=null) {
                        string+=attrs.getString(Tag.PatientName);
                    }
                    if(attrs.getString(Tag.PatientBirthDate)!=null) {
                        string+=", "+attrs.getString(Tag.PatientBirthDate);
                    }
                    if(attrs.getString(Tag.PatientID)!=null) {
                        string+=", "+attrs.getString(Tag.PatientID);
                    }
                    if(attrs.getString(Tag.StudyDate)!=null) {
                        string+=", "+attrs.getString(Tag.StudyDate);
                    }
                    log.debug("StudyInstanceUID unavailable, using "+string);
                    findSubRootByName(table, string).getChildren().add(new TreeItem<>(prop));
                }
            });
            table.getRoot().setExpanded(true);
        }
    }
}
