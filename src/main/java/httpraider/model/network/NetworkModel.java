package httpraider.model.network;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class NetworkModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, ProxyModel> proxies;
    private final Set<ConnectionModel> connections;

    public NetworkModel() {
        proxies = new LinkedHashMap<>();
        connections = new LinkedHashSet<>();
        ProxyModel client = new ProxyModel(ProxyModel.CLIENT_ID, "Client", "");
        client.setClient(true);
        proxies.put(ProxyModel.CLIENT_ID, client);
    }

    public Collection<ProxyModel> getProxies() {
        return proxies.values();
    }

    public ProxyModel getProxy(String id) {
        return proxies.get(id);
    }

    public void addProxy(ProxyModel proxy) {
        proxies.put(proxy.getId(), proxy);
    }

    public void removeProxy(String id) {
        proxies.remove(id);
        connections.removeIf(c -> c.hasProxy(id));
    }

    public void addConnection(String fromId, String toId) {
        connections.add(new ConnectionModel(fromId, toId));
    }

    public void removeConnection(String fromId, String toId) {
        connections.remove(new ConnectionModel(fromId, toId));
    }

    public void removeAllConnections(String proxyId) {
        connections.removeIf(c -> c.hasProxy(proxyId));
    }

    public Set<ConnectionModel> getConnections() {
        return Collections.unmodifiableSet(connections);
    }
}
