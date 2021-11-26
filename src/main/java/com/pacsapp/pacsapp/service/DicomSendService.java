package com.pacsapp.pacsapp.service;

import com.pacsapp.pacsapp.container.PACSServer;
import com.pacsapp.pacsapp.controller.SendController;
import com.pacsapp.pacsapp.util.DIMSEUtilities;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.*;
import org.dcm4che3.tool.common.CLIUtils;
import org.dcm4che3.tool.storescu.StoreSCU;
import org.dcm4che3.util.TagUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class DicomSendService {

    public static void sendToPacsServer(PACSServer pacsServer, File file, SendController sendController){
        if (file==null) {
            throw new IllegalArgumentException("File was null");
        }

        if (pacsServer==null) {
            throw new IllegalArgumentException("PACS Server was null");
        }

        try {
            //configure
            Device device = new Device("pacsapp");

            Connection connection = new Connection();
            device.addConnection(connection);

            ApplicationEntity ae = new ApplicationEntity("PACS App");
            device.addApplicationEntity(ae);
            ae.addConnection(connection);

            StoreSCU storeSCU = new StoreSCU(ae);

            storeSCU.getAAssociateRQ().setCalledAET(pacsServer.getAeTitle());
            Connection remoteConnection = storeSCU.getRemoteConnection();
            remoteConnection.setHostname(pacsServer.getHost());
            remoteConnection.setPort(pacsServer.getPort());


            storeSCU.setRspHandlerFactory(new StoreSCU.RSPHandlerFactory() {

                @Override
                public DimseRSPHandler createDimseRSPHandler(final File f) {

                    return new DimseRSPHandler(0) {

                        @Override
                        public void onDimseRSP(Association as, Attributes cmd, Attributes data) {
                            super.onDimseRSP(as, cmd, data);
                            DicomSendService.onCStoreRSP(cmd, f, sendController);
                        }
                    };
                }

            });

            String[] attributes = new String[0];
            storeSCU.setAttributes(new Attributes());
            CLIUtils.addAttributes(storeSCU.getAttributes(), attributes);

            List<String> paths = new ArrayList<>();

            paths.add(file.getAbsolutePath());

            storeSCU.scanFiles(paths, false);
            log.debug("file path: "+paths.get(0));

            //create executor
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            device.setExecutor(executorService);
            device.setScheduledExecutor(scheduledExecutorService);

            //open and send
            try {
                storeSCU.open();

                storeSCU.sendFiles();

            } finally {
                storeSCU.close();
                executorService.shutdown();
                scheduledExecutorService.shutdown();
            }

        } catch (Exception e) {
            log.error("Failed to send file",e);
            throw new IllegalStateException(e);
        }
    }

    private static void onCStoreRSP(Attributes cmd, File file, SendController sendController) {
        int status = cmd.getInt(Tag.Status, -1);
        switch (status) {
            case Status.Success:
                log.info("C-STORE success: {}, file={}", TagUtils.shortToHexString(status), file.getAbsolutePath());
                log.info(cmd.toString());
                sendController.response("File successfully sent to PACS");
                break;
            case Status.CoercionOfDataElements:
            case Status.ElementsDiscarded:
            case Status.DataSetDoesNotMatchSOPClassWarning:
                log.warn("C-STORE warning: {}, file={}", TagUtils.shortToHexString(status), file.getAbsolutePath());
                log.warn(cmd.toString());
                sendController.response("Warning: " + DIMSEUtilities.getStatusText(status));
                break;
            default:
                log.error("C-STORE error: {}, file={}", TagUtils.shortToHexString(status), file.getAbsolutePath());
                log.error(cmd.toString());
                sendController.response("Failed to send file to PACS: " + DIMSEUtilities.getStatusText(status));
        }
    }
}

