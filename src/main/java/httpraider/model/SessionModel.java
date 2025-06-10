package httpraider.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SessionModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private final List<StreamModel> streamModels;
    private int nameSuffix;

    public SessionModel(){
        this.id = UUID.randomUUID().toString();
        streamModels = new ArrayList<>();
        this.name = "Session";
        this.nameSuffix = 1;
    }

    public SessionModel(String name, int nameSuffix) {
        this.id = UUID.randomUUID().toString();
        streamModels = new ArrayList<>();
        this.name = name;
        this.nameSuffix = nameSuffix;
    }

    public String getId() {
        return id;
    }

    public List<StreamModel> getStreams() { return streamModels; }

    public void addStream(StreamModel s) {
        streamModels.add(s);
    }

    public void removeStream(StreamModel s) {
        streamModels.remove(s);
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
