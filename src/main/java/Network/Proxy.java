package Network;

import ProxyFinder.RequestSamples;
import Utils.*;
import burp.api.montoya.http.message.requests.HttpRequest;

import java.util.ArrayList;
import java.util.List;

public class Proxy extends NetworkComponent {

    private List<Connection> connections;

    public Proxy() {
        super();
        this.connections = new ArrayList<Connection>();
    }

    public Proxy(String path) {
        super();
        this.connections = new ArrayList<Connection>();
        this.basePath = path;
    }

/*    public Proxy(List<List<RequestSamples>> proxySamples, String path){
        Proxy current = this;
        for (int i = 0; i < proxySamples.size(); i++){
            current.samples = proxySamples.get(i);
            current.basePath = path;
            if ((i+1)<proxySamples.size()){
                current.children.add(new Proxy());
                current = current.children.get(0);
            }
        }
    }*/

    public void connect(NetworkComponent child){
        Connection conn = new Connection(this, child);
        this.connections.add(conn);
    }

    public void disconnect(Connection conn){
        this.connections.remove(conn);
    }

    public void addConnection(Connection conn) {
        connections.add(conn);
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public List<NetworkComponent> getChildren() {
        List<NetworkComponent> out = new ArrayList<>();
        for (Connection con : connections){
            out.add(con.getEnd());
        }
        return out;
    }

    public void mergePath(String path) {
        this.basePath = Utils.getCommonDirectory(this.basePath, path);
    }
}
