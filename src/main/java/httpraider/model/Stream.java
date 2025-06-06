package httpraider.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public class Stream implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private byte[] clientRequest;
    private byte[] requestQueue;
    private byte[] responseQueue;
    private ConnectionSettings connectionSettings;

    public Stream(){
        id = UUID.randomUUID().toString();
        connectionSettings = new ConnectionSettings();
        clientRequest = new byte[0];
        requestQueue = new byte[0];
        responseQueue = new byte[0];
        name = "";
    }

    public Stream(String name) {
        id = UUID.randomUUID().toString();
        connectionSettings = new ConnectionSettings();
        clientRequest = new byte[0];
        requestQueue = new byte[0];
        responseQueue = new byte[0];
        this.name = name;
    }

    public Stream(String name, byte[] clientRequest, byte[] requestQueue, byte[] responseQueue, ConnectionSettings connectionSettings) {
        id = UUID.randomUUID().toString();
        this.clientRequest = clientRequest;
        this.requestQueue = requestQueue;
        this.responseQueue = responseQueue;
        this.connectionSettings = connectionSettings;
        this.name = name;
    }

    public String getId() { return id; }

    public byte[] getClientRequest() {
        return clientRequest;
    }

    public void setClientRequest(byte[] v) {
        clientRequest = v;
    }

    public byte[] getRequestQueue() {
        return requestQueue;
    }

    public void setRequestQueue(byte[] v) {
        requestQueue = v;
    }

    public byte[] getResponseQueue() {
        return responseQueue;
    }

    public void setResponseQueue(byte[] v) {
        responseQueue = v;
    }

    public ConnectionSettings getConnectionSettings() {
        return connectionSettings;
    }

    public void setConnectionSettings(ConnectionSettings v) {
        connectionSettings = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
