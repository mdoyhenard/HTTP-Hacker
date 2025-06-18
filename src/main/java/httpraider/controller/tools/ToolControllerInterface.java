package httpraider.controller.tools;

import javax.swing.*;

public interface ToolControllerInterface {

    String id();

    String name();

    JComponent component();

    void attach();

}