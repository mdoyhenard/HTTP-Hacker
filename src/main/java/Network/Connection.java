package Network;

import Network.View.Nodes.NetworkNode;

public class Connection {
    private NetworkComponent start;
    private NetworkComponent end;
    private boolean tls = false;
    private boolean pipelining = false;
    private boolean http2 = false;
    private boolean selected = false;
    private String description;

    public Connection(NetworkComponent start, NetworkComponent end) {
        this.start = start;
        this.end = end;
    }

    public NetworkComponent getStart() {
        return start;
    }

    public NetworkComponent getEnd() {
        return end;
    }

    public boolean isTls() {
        return tls;
    }

    public void setTls(boolean tls) {
        this.tls = tls;
    }

    public boolean isPipelining() {
        return pipelining;
    }

    public void setPipelining(boolean pipelining) {
        this.pipelining = pipelining;
    }

    public boolean isHttp2() {
        return http2;
    }

    public void setHttp2(boolean http2) { this.http2 = http2; }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setStart(NetworkComponent start) {
        this.start = start;
    }

    public void setEnd(NetworkComponent end) {
        this.end = end;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
