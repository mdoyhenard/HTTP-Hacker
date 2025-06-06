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
    private String name;
    private final List<Stream> streams;
    private int nameSuffix;

    public Session(){
        this.id = UUID.randomUUID().toString();
        streams = new ArrayList<>();
        this.name = "Session";
        this.nameSuffix = 1;
    }

    public Session(String name, int nameSuffix) {
        this.id = UUID.randomUUID().toString();
        streams = new ArrayList<>();
        this.name = name;
        this.nameSuffix = nameSuffix;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNameSuffix() {
        return nameSuffix;
    }

    public void setNameSuffix(int nameSuffix) {
        this.nameSuffix = nameSuffix;
    }
}
