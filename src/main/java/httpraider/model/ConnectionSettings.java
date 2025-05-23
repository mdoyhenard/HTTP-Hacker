package httpraider.model;

import java.io.Serial;
import java.io.Serializable;

public class ConnectionSettings implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private String host;
    private int port;
    private boolean tls;
    private boolean reset;

    public ConnectionSettings() { }

    public ConnectionSettings(String host, int port, boolean tls, boolean reset) {
        this.host = host;
        this.port = port;
        this.tls = tls;
        this.reset = reset;
    }

    public String getHost(){ return host;}
    public int getPort(){ return port;}
    public boolean isTls(){ return tls;}
    public boolean isReset(){ return reset;}

    public void setHost(String host){ this.host = host;}
    public void setPort(int port){ this.port = port;}
    public void setTls(boolean tls){ this.tls = tls;}
    public void setReset(boolean reset){ this.reset = reset;}
}
