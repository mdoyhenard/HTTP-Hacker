package Network;

import java.util.ArrayList;
import java.util.List;

public class Server extends NetworkComponent{

    private String language;
    private List<String> frameworks;
    private List<String> delimiters;

    public Server(){
        super();
        this.language = "";
        this.frameworks = new ArrayList<>();
        this.delimiters = new ArrayList<>();
    }
}
