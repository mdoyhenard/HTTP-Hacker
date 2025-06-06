package httpraider.controller;

import javax.swing.*;

public interface InspectorTool {

    String id();

    String name();

    JComponent component();

    void attach();

}