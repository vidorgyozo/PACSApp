package com.pacsapp.pacsapp.concurrent;

import com.pacsapp.pacsapp.container.PACSServer;
import com.pacsapp.pacsapp.service.DicomFindService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FindOnPACSService extends Service<Void> {

    private PACSServer server;
    private String[] keys;
    private String retrieveLevel;

    public FindOnPACSService(PACSServer server,String[] keys, String retrieveLevel) {
        super();
        this.server=server;
        this.keys=keys;
        this.retrieveLevel=retrieveLevel;
    }

    @Override
    protected Task<Void> createTask() {
        return new FindOnPACSTask();
    }

    private class FindOnPACSTask extends Task<Void> {

        @Override
        protected Void call() {
            try {
                DicomFindService.findRequestToPacsServer(server, keys, retrieveLevel);
            } catch (Exception e) {
                log.error("Failed to send find request to PACS server",e);
                throw new IllegalStateException(e);
            }
            return null;
        }
    }
}
