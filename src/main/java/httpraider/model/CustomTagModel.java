// src/httpraider/model/CustomTagDefinition.java
package httpraider.model;

import java.io.Serial;
import java.io.Serializable;

public class CustomTagModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String script;

    public CustomTagModel(String name, String script) {
        this.name = name;
        this.script = script;
    }

    public String getName() {
        return name;
    }

    public String getScript() {
        return script;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScript(String script) {
        this.script = script;
    }
}
