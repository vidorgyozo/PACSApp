package com.pacsapp.pacsapp.concurrent;

import com.pacsapp.pacsapp.container.PACSServer;
import com.pacsapp.pacsapp.controller.DownloadController;
import com.pacsapp.pacsapp.service.DicomFindService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadFromPACSService extends Service<Void> {

    private PACSServer server;
    private String[] keys;
    private String retrieveLevel;
    private DownloadController downloadController;

    public DownloadFromPACSService(PACSServer server, String[] keys, String retrieveLevel, DownloadController downloadController) {
        this.server = server;
        this.keys = keys;
        this.retrieveLevel = retrieveLevel;
        this.downloadController = downloadController;
    }

    @Override
    protected Task<Void> createTask() {
        return new DownloadFromPACSTask();
    }

    private class DownloadFromPACSTask extends Task<Void> {

        @Override
        protected Void call() {
            try {
                DicomFindService.downloadFromPacsServer(server, keys, retrieveLevel, downloadController);
            } catch (Exception e){
                log.error("Failed to download files from PACS server");
                throw new IllegalStateException(e);
            }
            return null;
        }
    }
}
