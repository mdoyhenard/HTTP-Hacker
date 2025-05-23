package httpraider.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public class Stream implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String id;

    private byte[] clientRequest;
    private byte[] requestQueue;
    private byte[] responseQueue;
    private ConnectionSettings connectionSettings;

    public Stream(){
        id = UUID.randomUUID().toString();
        clientRequest = null;
        requestQueue = null;
        responseQueue = null;
        connectionSettings = new ConnectionSettings();
    }

    public Stream(byte[] clientRequest, byte[] requestQueue, byte[] responseQueue, ConnectionSettings connectionSettings){
        this.id = UUID.randomUUID().toString();
        this.clientRequest = clientRequest;
        this.requestQueue = requestQueue;
        this.responseQueue = responseQueue;
        this.connectionSettings = connectionSettings;
    }

    public String getId() { return id; }

    public byte[] getClientRequest() {
        return clientRequest;
    }

    public void setClientRequest(byte[] clientRequest) {
        this.clientRequest = clientRequest;
    }

    public byte[] getRequestQueue() {
        return requestQueue;
    }

    public void setRequestQueue(byte[] requestQueue) {
        this.requestQueue = requestQueue;
    }

    public byte[] getResponseQueue() {
        return responseQueue;
    }

    public void setResponseQueue(byte[] responseQueue) {
        this.responseQueue = responseQueue;
    }

    public ConnectionSettings getConnectionSettings() {
        return connectionSettings;
    }

    public void setConnectionSettings(ConnectionSettings connectionSettings) {
        this.connectionSettings = connectionSettings;
    }
}
