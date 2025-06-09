package extension;

import httpraider.controller.StreamController;
import httpraider.controller.tools.EditorToolController;
import httpraider.controller.ToolControllerInterface;
import httpraider.view.menuBars.InspectorBar;
import httpraider.view.panels.StreamPanel;

import java.util.ArrayList;
import java.util.List;

public class ToolsManager {

    private final List<ToolControllerInterface> gadgets = new ArrayList<>();

    public ToolsManager(StreamPanel view, StreamController controller) {
        gadgets.add(new EditorToolController(
                view.getEditorToolsPanel(),
                view.getClientRequestEditor(), controller));
        InspectorBar bar = view.getInspectorBar();
        gadgets.forEach(g -> bar.addTool(g.id(), g.name(), g.component()));
        gadgets.forEach(ToolControllerInterface::attach);
    }
}
