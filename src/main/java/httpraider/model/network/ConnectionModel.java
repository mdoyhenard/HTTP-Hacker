package httpraider.model.network;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class ConnectionModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fromId;
    private final String toId;

    public ConnectionModel(String fromId, String toId) {
        this.fromId = fromId;
        this.toId = toId;
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }

    public boolean hasProxy(String id) {
        return fromId.equals(id) || toId.equals(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConnectionModel)) return false;
        ConnectionModel that = (ConnectionModel) o;
        return Objects.equals(fromId, that.fromId) && Objects.equals(toId, that.toId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromId, toId);
    }
}
