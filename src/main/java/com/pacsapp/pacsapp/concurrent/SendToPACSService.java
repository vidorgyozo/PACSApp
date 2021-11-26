package com.pacsapp.pacsapp.concurrent;

import com.pacsapp.pacsapp.container.PACSServer;
import com.pacsapp.pacsapp.controller.SendController;
import com.pacsapp.pacsapp.service.DicomSendService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class SendToPACSService extends Service<Void> {

    private PACSServer pacsServer;
    private File dicomFile;
    private SendController sendController;

    public SendToPACSService(PACSServer pacsServer, File dicomFile, SendController sendController){
        super();
        this.pacsServer = pacsServer;
        this.dicomFile = dicomFile;
        this.sendController = sendController;
    }

    @Override
    protected Task<Void> createTask() {
        return new SendToPACSTask();
    }

    private class SendToPACSTask extends Task<Void>{

        @Override
        protected Void call() {
            try {
                DicomSendService.sendToPacsServer(pacsServer, dicomFile, sendController);
            } catch (Exception e){
                log.error("Failed to send store request to PACS server",e);
                throw new IllegalStateException(e);
            }
            return null;
        }
    }
}
