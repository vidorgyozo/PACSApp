package com.pacsapp.pacsapp.service;

import com.pacsapp.pacsapp.container.PACSServer;
import com.pacsapp.pacsapp.controller.DownloadController;
import com.pacsapp.pacsapp.util.DIMSEUtilities;
import com.pacsapp.pacsapp.util.FileUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.tool.common.CLIUtils;
import org.dcm4che3.tool.findscu.FindSCU;
import org.dcm4che3.tool.getscu.GetSCU;
import org.dcm4che3.util.StringUtils;
import org.dcm4che3.util.TagUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class DicomFindService {

    private static final String[] EMPTY_ATTRIBUTES= {
            "PatientName","StudyDate","ModalitiesInStudy","SOPInstanceUID",
            "StudyInstanceUID","PatientID","PatientBirthDate","NumberOfFrames", "DerivationDescription"};

    public static void findRequestToPacsServer(PACSServer pacsServer,String[] keys, String retrieveLevel) throws IOException {

        if (pacsServer==null) {
            throw new IllegalArgumentException("PACS Server was null");
        }

        try {
            FindSCU findScu = new FindSCU();

            //configure
            findScu.getAAssociateRQ().setCalledAET(pacsServer.getAeTitle());
            Connection remoteConnection = findScu.getRemoteConnection();
            remoteConnection.setHostname(pacsServer.getHost());
            remoteConnection.setPort(pacsServer.getPort());

            findScu.getDevice().setDeviceName("pacsapp");
            findScu.getApplicationEntity().setAETitle("PACS App");

            String[] EVR_LE_FIRST = {
                    UID.ExplicitVRLittleEndian,
                    UID.ExplicitVRBigEndian,
                    UID.ImplicitVRLittleEndian};
            findScu.setInformationModel(FindSCU.InformationModel.StudyRoot,
                    EVR_LE_FIRST, EnumSet.noneOf(QueryOption.class));
            if(retrieveLevel.equals("IMAGE")){
                findScu.addLevel("IMAGE");
            }

            Attributes attrs=findScu.getKeys();
            CLIUtils.addEmptyAttributes(attrs, EMPTY_ATTRIBUTES);
            CLIUtils.addAttributes(attrs, keys);

            log.debug(String.valueOf(attrs));
            log.debug(Arrays.toString(keys));

            log.debug("Setting temporary output directory and file format");
            String dirPath = FileUtilities.getPACSAppUserFolder().getAbsolutePath();
            File queryTmpDir = new File(FilenameUtils.concat(dirPath, "tmp/query/"));
            findScu.setOutputDirectory(queryTmpDir);
            FileUtils.cleanDirectory(queryTmpDir);
            findScu.setOutputFileFormat("000'.dcm'");
            log.debug("Temporary output directory and file format set");

            //create executor
            ExecutorService executorService =
                    Executors.newSingleThreadExecutor();
            ScheduledExecutorService scheduledExecutorService =
                    Executors.newSingleThreadScheduledExecutor();
            findScu.getDevice().setExecutor(executorService);
            findScu.getDevice().setScheduledExecutor(scheduledExecutorService);

            //open and send
            try {
                log.info("Opening connection for C-FIND");
                findScu.open();
                log.info("Connection opened");

                log.info("Sending C-FIND");
                findScu.query();
                log.info("C-FIND query sent");
            } finally {
                findScu.close();
                executorService.shutdown();
                scheduledExecutorService.shutdown();
            }
        } catch (Exception e) {
            log.error("Find query failed", e);
            throw new IllegalStateException(e);
        }
    }


    public static void downloadFromPacsServer(PACSServer pacsServer, String[] keys, String retrieveLevel, DownloadController downloadController) {
        getScu(pacsServer, keys, retrieveLevel, downloadController);
    }

    private static void getScu(PACSServer pacsServer, String[] keys, String retrieveLevel, DownloadController downloadController) {
        try {
            log.info("Retrieving file from PACS with C-GET...");
            GetSCU getScu=new GetSCU();

            //configure
            getScu.getAAssociateRQ().setCalledAET(pacsServer.getAeTitle());
            Connection remote = getScu.getRemoteConnection();
            remote.setHostname(pacsServer.getHost());
            remote.setPort(pacsServer.getPort());

            getScu.getDevice().setDeviceName("pacsapp");
            getScu.getApplicationEntity().setAETitle("PACS App");


            String[] EVR_LE_FIRST = {
                    UID.ExplicitVRLittleEndian,
                    UID.ExplicitVRBigEndian,
                    UID.ImplicitVRLittleEndian};
            getScu.setInformationModel(GetSCU.InformationModel.StudyRoot,
                    EVR_LE_FIRST, false);

            Field[] fields = UID.class.getFields();

            for (int i = 0; i < fields.length; i++) {
                String uid = (String)fields[i].get(null);
                getScu.getAAssociateRQ().addPresentationContext(new PresentationContext(2 + i, uid,
                        UID.ExplicitVRLittleEndian,
                        UID.ExplicitVRBigEndian,
                        UID.ImplicitVRLittleEndian));
            }

            if(retrieveLevel.equals("IMAGE")){
                getScu.addLevel("IMAGE");
            }

            for (int i = 1; i < keys.length; i++, i++) {
                getScu.addKey(CLIUtils.toTag(keys[i - 1]), StringUtils.split(keys[i], '/'));
            }

            log.info(Arrays.toString(keys));
            log.info(String.valueOf(getScu.getKeys()));

            String dirPath = FileUtilities.getPACSAppUserFolder().getAbsolutePath();
            File outputDir = new File(FilenameUtils.concat(dirPath,"downloaded"));
            getScu.setStorageDirectory(outputDir);

            //create executor
            ExecutorService executorService =
                    Executors.newSingleThreadExecutor();
            ScheduledExecutorService scheduledExecutorService =
                    Executors.newSingleThreadScheduledExecutor();
            getScu.getDevice().setExecutor(executorService);
            getScu.getDevice().setScheduledExecutor(scheduledExecutorService);

            //open and send
            try {
                log.info("Opening connection for C-GET");
                getScu.open();
                log.info("Connection opened");

                log.info("Sending C-GET");
                getScu.retrieve(new DimseRSPHandler(0){
                    @Override
                    public void onDimseRSP(Association as, Attributes cmd, Attributes data) {
                        super.onDimseRSP(as, cmd, data);
                        DicomFindService.onCGetRSP(cmd, downloadController);
                    }
                });
                log.info("C-GET query sent");
            } finally {
                getScu.close();
                executorService.shutdown();
                scheduledExecutorService.shutdown();
            }
        } catch (Exception e) {
            log.error("Retrieve failed", e);
            throw new IllegalStateException(e);
        }
    }

    private static void onCGetRSP(Attributes cmd, DownloadController downloadController){
        int status = cmd.getInt(Tag.Status, -1);
        switch (status) {
            case Status.Pending:
                log.info("C-GET response: PENDING ({})", TagUtils.shortToHexString(status), Status.isPending(status));
                log.info(cmd.toString());
                break;
            case Status.Success:
                log.info("C-GET response: SUCCESS ({})", TagUtils.shortToHexString(status), Status.isPending(status));
                log.info(cmd.toString());
                downloadController.response("Successfully downloaded file(s) from PACS");
                break;
            case Status.CoercionOfDataElements:
            case Status.ElementsDiscarded:
            case Status.DataSetDoesNotMatchSOPClassWarning:
            case Status.OutOfResources:
            case Status.AttributeListError:
            case Status.AttributeValueOutOfRange:
                log.warn("C-GET response: WARNING ({})", TagUtils.shortToHexString(status));
                log.warn(cmd.toString());
                break;
            default:
                log.error("C-GET response: ERROR ({})", TagUtils.shortToHexString(status));
                log.error(cmd.toString());
                downloadController.response("Failed to download file(s) from PACS: " + DIMSEUtilities.getStatusText(status));
        }
    }

}
