package httpraider.model;

import java.io.Serial;
import java.io.Serializable;

public class ConnectionSettingsModel implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private String host;
    private int port;
    private boolean tls;

    public ConnectionSettingsModel() {
        host="";
        port=443;
        tls=true;
    }

    public ConnectionSettingsModel(String host, int port, boolean tls) {
        this.host = host;
        this.port = port;
        this.tls = tls;
    }

    public String getHost(){ return host;}
    public int getPort(){ return port;}
    public boolean isTls(){ return tls;}

    public void setHost(String host){ this.host = host;}
    public void setPort(int port){ this.port = port;}
    public void setTls(boolean tls){ this.tls = tls;}
}
