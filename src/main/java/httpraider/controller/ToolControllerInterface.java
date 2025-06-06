package httpraider.controller;

import javax.swing.*;

public interface ToolControllerInterface {

    String id();

    String name();

    JComponent component();

    void attach();

}