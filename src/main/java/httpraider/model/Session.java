package httpraider.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Session implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final List<Stream> streams;

    public Session() {
        this.id = UUID.randomUUID().toString();
        streams = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public List<Stream> getStreams() {
        return streams;
    }

    public void addStream(Stream s) {
        streams.add(s);
    }

    public void removeStream(Stream s) {
        streams.remove(s);
    }
}
