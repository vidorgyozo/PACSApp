package com.pacsapp.pacsapp.container;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PACSServer {

    private String aeTitle;
    private String host;
    private int port;

    public PACSServer(String aeTitle, String host, int port) {
        this.aeTitle = aeTitle;
        this.host = host;
        this.port = port;
    }
}
