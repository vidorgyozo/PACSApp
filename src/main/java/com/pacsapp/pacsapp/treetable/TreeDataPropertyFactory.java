package com.pacsapp.pacsapp.treetable;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;

import java.util.Arrays;
import java.util.List;

public class TreeDataPropertyFactory {
    public List<TreeDataProperty> createPacsDownloadTreeData(Attributes attrs) {
        return Arrays.asList(new TreeDataProperty("Patient Name", attrs.getString(Tag.PatientName)),
                new TreeDataProperty("Patient ID", attrs.getString(Tag.PatientID)),
                new TreeDataProperty("Patient Birth Date", attrs.getString(Tag.PatientBirthDate)),
                new TreeDataProperty("Study Date", attrs.getString(Tag.StudyDate)),
                new TreeDataProperty("Modalities in Study", attrs.getString(Tag.ModalitiesInStudy)),
                new TreeDataProperty("Number of Frames", attrs.getString(Tag.NumberOfFrames))
        );
    }
}
