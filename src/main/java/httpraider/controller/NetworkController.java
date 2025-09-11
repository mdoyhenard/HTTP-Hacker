package httpraider.controller;

import httpraider.view.panels.DiscoverNetworkPanel;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import extension.HTTPRaiderExtension;
import httpraider.model.network.ConnectionModel;
import httpraider.model.network.NetworkModel;
import httpraider.model.network.ProxyModel;
import httpraider.parser.ParserChainRunner;
import httpraider.utils.ProxyExporter;
import httpraider.view.components.*;
import httpraider.view.components.network.ConnectionLine;
import httpraider.view.components.network.ProxyComponent;
import httpraider.view.panels.*;
import httpraider.view.menuBars.NetworkBar;
import httpraider.view.menuBars.ProxyBar;
import httpraider.view.panels.network.NetworkCanvas;
import httpraider.view.panels.network.NetworkPanel;
import httpraider.view.panels.HttpParserPanel;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class NetworkController {

    private final NetworkModel model;
    private final NetworkPanel view;
    private final NetworkCanvas canvas;
    private final NetworkBar networkBar;
    private final ProxyBar proxyBar;
    private final Map<String, ProxyController> proxyControllers;
    private final Map<String, Point> positions;
    private final List<StreamController> streamControllers;
    private Point pan;
    private boolean isPanning;
    private Point panStart;
    private Component pressedComponent;
    private ProxyComponent draggingProxy;
    private Point dragOffset;
    private ProxyComponent connectStartProxy;
    private boolean isConnecting;
    private String selectedProxyId;
    private int selectedReqId = 0;
    private HttpParserController parserController;
    private final HttpEditorPanel<HttpRequestEditor> reqEditor;
    private HttpMultiEditorPanel cachedParsedRequestPanel;
    private StreamComboBox<byte[]> cachedStreamsBox;


    public static final int ICON_WIDTH = 47;
    public static final int ICON_HEIGHT = 65;

    public NetworkController(NetworkModel model, NetworkPanel view, List<StreamController> streamControllers) {
        this.model = model;
        this.view = view;
        this.canvas = view.getNetworkCanvas();
        this.networkBar = view.getNetworkBar();
        this.proxyBar = view.getProxyBar();
        this.proxyControllers = new HashMap<>();
        this.positions = new HashMap<>();
        this.streamControllers = streamControllers;
        this.pan = new Point(0, 0);
        this.isPanning = false;
        this.panStart = null;
        this.pressedComponent = null;
        this.draggingProxy = null;
        this.dragOffset = null;
        this.connectStartProxy = null;
        this.isConnecting = false;
        this.selectedProxyId = null;
        this.reqEditor = new HttpEditorPanel<>("Client Request",
                HTTPRaiderExtension.API.userInterface().createHttpRequestEditor());


        for (ProxyModel proxy : model.getProxies()) {
            ProxyComponent proxyComponent = new ProxyComponent(proxy.getId(), proxy.isClient(), proxy.isShowParser());
            ProxyController controller = new ProxyController(proxy, proxyComponent);
            proxyControllers.put(proxy.getId(), controller);
        }

        canvas.setPan(pan);
        reloadAll(true);

        canvas.addCanvasMouseListener(canvasMouseListener);
        canvas.addCanvasMouseMotionListener(canvasMouseMotionListener);

        networkBar.setAutoLayoutActionListener(e -> reloadAll(true));
        networkBar.setDiscoverActionListener(e -> {
            DiscoverNetworkPanel panel = new DiscoverNetworkPanel(HTTPRaiderExtension.API, this);
            JDialog dialog = new JDialog((Frame) null, "Network Discovery", true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setContentPane(panel);
            dialog.setSize(800, 500);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });
        view.setProxyBarVisible(false);
        installBarListeners();
    }

    private void reloadAll(boolean layoutOnLoad) {
        List<ProxyModel> proxies = new ArrayList<>(model.getProxies());
        if (layoutOnLoad) {
            positions.clear();
            positions.putAll(generateLayout(proxies));
        }
        
        canvas.clear();
        
        for (ProxyModel proxy : proxies) {
            ProxyController controller = proxyControllers.get(proxy.getId());
            if (controller == null) {
                ProxyComponent proxyComponent = new ProxyComponent(proxy.getId(), proxy.isClient(), proxy.isShowParser());
                ProxyController pc = new ProxyController(proxy, proxyComponent);
                proxyControllers.put(proxy.getId(), pc);
                controller = pc;
            }
            ProxyComponent pv = controller.getView();
            Point pos = positions.get(proxy.getId());
            if (pos == null) {
                pos = new Point(350, 250);
                positions.put(proxy.getId(), pos);
            }
            pv.setSize(ICON_WIDTH, ICON_HEIGHT);
            Point displayPos = new Point(pos.x + pan.x, pos.y + pan.y);
            pv.setLocation(displayPos.x, displayPos.y);
            pv.setSelected(proxy.getId().equals(selectedProxyId));
            canvas.addProxyView(proxy.getId(), pv, displayPos);
            installProxyListeners(controller);
        }
        for (ConnectionModel c : model.getConnections()) {
            ProxyComponent from = canvas.getProxyView(c.getFromId());
            ProxyComponent to = canvas.getProxyView(c.getToId());
            if (from != null && to != null) {
                canvas.addConnectionView(new ConnectionLine(from, to));
            }
        }
        canvas.setConnectStartProxy(isConnecting ? connectStartProxy : null);
        canvas.setMousePoint(null);
        canvas.setHighlightedConnection(null);
        updateProxyBarVisibility();
    }

    private Map<String, Point> generateLayout(List<ProxyModel> proxies) {
        int baseX = 50, gapX = 220, gapY = 120, baseY = gapY;
        String clientId = null;
        for (ProxyModel p : proxies) {
            if (p.isClient()) { clientId = p.getId(); break; }
        }
        if (clientId == null) return new HashMap<>();
        Map<String, Set<String>> adj = new HashMap<>();
        for (ProxyModel p : proxies) adj.put(p.getId(), new HashSet<>());
        for (ConnectionModel c : model.getConnections()) {
            adj.get(c.getFromId()).add(c.getToId());
            adj.get(c.getToId()).add(c.getFromId());
        }
        Map<String, List<String>> children = new HashMap<>();
        Map<String, Integer> level = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(clientId);
        level.put(clientId, 0);
        visited.add(clientId);
        while (!queue.isEmpty()) {
            String id = queue.poll();
            for (String nbr : adj.get(id)) {
                if (!visited.contains(nbr)) {
                    visited.add(nbr);
                    level.put(nbr, level.get(id) + 1);
                    children.computeIfAbsent(id, k -> new ArrayList<>()).add(nbr);
                    queue.add(nbr);
                }
            }
        }
        Map<String, Integer> nodeY = new HashMap<>();
        int[] leafCounter = {0};
        class Positioner {
            int assign(String id) {
                List<String> kid = children.getOrDefault(id, Collections.emptyList());
                if (kid.isEmpty()) {
                    nodeY.put(id, leafCounter[0]++);
                    return nodeY.get(id);
                }
                int first = Integer.MAX_VALUE, last = Integer.MIN_VALUE;
                for (String c : kid) {
                    int y = assign(c);
                    first = Math.min(first, y);
                    last = Math.max(last, y);
                }
                int mid = (first + last) / 2;
                nodeY.put(id, mid);
                return mid;
            }
        }
        new Positioner().assign(clientId);
        Map<String, Integer> depthOf = new HashMap<>();
        Map<String, Integer> yIndexOf = new HashMap<>();
        Map<Integer, Set<Integer>> usedY = new HashMap<>();
        for (ProxyModel p : proxies) {
            String id = p.getId();
            if (nodeY.containsKey(id)) {
                int d = level.getOrDefault(id, 0);
                int yIdx = nodeY.get(id);
                depthOf.put(id, d);
                yIndexOf.put(id, yIdx);
                usedY.computeIfAbsent(d, k -> new HashSet<>()).add(yIdx);
            }
        }
        int maxBaseDepth = level.values().stream().max(Integer::compareTo).orElse(0);
        int extraCount = 0;
        for (ProxyModel p : proxies) {
            String id = p.getId();
            if (!depthOf.containsKey(id)) {
                int d = maxBaseDepth + 1 + extraCount;
                depthOf.put(id, d);
                Set<Integer> used = usedY.computeIfAbsent(d, k -> new HashSet<>());
                int yIdx = 0;
                while (used.contains(yIdx)) yIdx++;
                used.add(yIdx);
                yIndexOf.put(id, yIdx);
                extraCount++;
            }
        }
        Map<String, Set<String>> direct = new HashMap<>();
        for (ProxyModel p : proxies) direct.put(p.getId(), new HashSet<>());
        for (ConnectionModel c : model.getConnections()) {
            direct.get(c.getFromId()).add(c.getToId());
            direct.get(c.getToId()).add(c.getFromId());
        }
        List<String> toPush = new ArrayList<>();
        for (String id : depthOf.keySet()) {
            Integer baseLvl = level.get(id);
            if (baseLvl == null) continue;
            List<String> sameLvl = new ArrayList<>();
            for (String nbr : adj.get(id)) {
                if (Objects.equals(level.get(nbr), baseLvl)) {
                    sameLvl.add(nbr);
                }
            }
            if (sameLvl.size() < 2) continue;
            boolean triangle = false;
            for (int i = 0; i < sameLvl.size() && !triangle; i++) {
                for (int j = i + 1; j < sameLvl.size(); j++) {
                    if (direct.get(sameLvl.get(i)).contains(sameLvl.get(j))) {
                        triangle = true;
                        break;
                    }
                }
            }
            if (triangle) toPush.add(id);
        }
        for (String id : toPush) {
            int oldD = depthOf.get(id);
            int newD = oldD + 1;
            int yIdx = yIndexOf.get(id);
            usedY.get(oldD).remove(yIdx);
            depthOf.put(id, newD);
            Set<Integer> usedNew = usedY.computeIfAbsent(newD, k -> new HashSet<>());
            if (usedNew.contains(yIdx)) {
                for (int delta = 1; delta < proxies.size(); delta++) {
                    int low = yIdx - delta;
                    int high = yIdx + delta;
                    if (low >= 0 && !usedNew.contains(low)) {
                        yIdx = low;
                        break;
                    }
                    if (!usedNew.contains(high)) {
                        yIdx = high;
                        break;
                    }
                }
            }
            usedNew.add(yIdx);
            yIndexOf.put(id, yIdx);
        }
        Map<String, Point> result = new HashMap<>();
        for (String id : depthOf.keySet()) {
            int d = depthOf.get(id);
            int yIdx = yIndexOf.get(id);
            int x = baseX + d * gapX;
            int y = baseY + yIdx * gapY;
            result.put(id, new Point(x, y));
        }
        return result;
    }

    private void updateProxyViewLocations() {
        for (ProxyController pc : proxyControllers.values()) {
            ProxyComponent pv = pc.getView();
            Point pos = positions.get(pc.getId());
            if (pv != null && pos != null) {
                pv.setLocation(pos.x + pan.x, pos.y + pan.y);
            }
        }
    }

    private void installBarListeners() {
        proxyBar.addDescriptionListener(new DocumentListener() {
            private void update() {
                if (selectedProxyId != null) {
                    proxyControllers.get(selectedProxyId).setDescription(proxyBar.getDescription());
                }
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

        proxyBar.addParsingCodeListener(e -> {
            if (selectedProxyId == null) return;
            ProxyController pc = proxyControllers.get(selectedProxyId);
            
            // Find the top-level window containing this component
            Window parentWindow = SwingUtilities.getWindowAncestor(view);
            if (parentWindow == null) {
                // Try to get window from the event source
                Component source = (Component) e.getSource();
                parentWindow = SwingUtilities.getWindowAncestor(source);
            }
            
            HttpParserPanel panel = new HttpParserPanel(parentWindow);
            // Set the current client request from the network panel
            panel.setInitialRequest(reqEditor.getBytes());
            this.parserController = new HttpParserController(
                    pc.getModel(),
                    panel,
                    this
            );
            panel.setVisible(true);
        });

        proxyBar.addShowInStreamsListener(e -> {
            showParserInStreams(selectedProxyId, ((JToggleButton)e.getSource()).isSelected());
        });

        proxyBar.addDomainNameListener(new DocumentListener() {
            private void update() {
                if (selectedProxyId != null) {
                    proxyControllers.get(selectedProxyId).setDomainName(proxyBar.getDomainName());
                }
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });
        
        proxyBar.addExportListener(e -> {
            if (selectedProxyId == null) return;
            ProxyController pc = proxyControllers.get(selectedProxyId);
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Proxy Configuration");
            fileChooser.setSelectedFile(new java.io.File(pc.getDomainName() + "_proxy.json"));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json"));
            
            if (fileChooser.showSaveDialog(view) == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".json")) {
                    file = new java.io.File(file.getAbsolutePath() + ".json");
                }
                
                try {
                    ProxyExporter.exportProxy(pc.getModel(), file);
                    JOptionPane.showMessageDialog(view, "Proxy configuration exported successfully!", 
                        "Export Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, "Error exporting proxy: " + ex.getMessage(), 
                        "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void showParserInStreams(String proxyId, boolean show){
        model.getProxy(proxyId).setShowParser(show);
        proxyControllers.get(proxyId).setEnabledProxy(show);
        for (StreamController streamController : streamControllers){
            streamController.resetView();
        }
    }

    private void installProxyListeners(ProxyController controller) {
        ProxyComponent pv = controller.getView();
        MouseListener[] oldML = pv.getMouseListeners();
        MouseMotionListener[] oldMML = pv.getMouseMotionListeners();
        for (MouseListener l : oldML) pv.removeMouseListener(l);
        for (MouseMotionListener l : oldMML) pv.removeMouseMotionListener(l);

        final Timer[] singleClickTimer = {null};

        controller.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showProxyMenu(pv, e.getXOnScreen(), e.getYOnScreen());
                    return;
                }
                pressedComponent = pv;
                if (SwingUtilities.isLeftMouseButton(e)) {
                    draggingProxy = pv;
                    dragOffset = e.getPoint();
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) return;
                draggingProxy = null;
                dragOffset = null;
            }
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (isConnecting && connectStartProxy != null && pv != connectStartProxy) {
                    model.addConnection(connectStartProxy.getId(), pv.getId());
                    canvas.addConnectionView(new ConnectionLine(connectStartProxy, pv));
                    isConnecting = false;
                    connectStartProxy = null;
                    canvas.setConnectStartProxy(null);
                    canvas.setMousePoint(null);
                    return;
                }
                
                if (!isConnecting && !pv.isClient()) {
                    if (pv.getId().equals(selectedProxyId)) {
                        unselectProxy();
                    } else {
                        selectProxy(pv.getId());
                    }
                }
            }
        });
        controller.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (draggingProxy != null) {
                    int nx = draggingProxy.getX() + e.getX() - dragOffset.x;
                    int ny = draggingProxy.getY() + e.getY() - dragOffset.y;
                    positions.put(
                            draggingProxy.getId(),
                            new Point(nx - pan.x, ny - pan.y)
                    );
                    draggingProxy.setLocation(nx, ny);
                    canvas.scheduleRepaint();
                }
            }
        });
    }

    private final MouseListener canvasMouseListener = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                ConnectionLine conn = canvas.getHighlightedConnection();
                if (conn != null) {
                    showConnectionMenu(conn, e.getX(), e.getY());
                } else {
                    showBackgroundMenu(e.getX(), e.getY());
                }
                return;
            }
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            pressedComponent = canvas.getComponentAt(e.getPoint());
            if (!(pressedComponent instanceof ProxyComponent)) {
                isPanning = true;
                panStart = e.getPoint();
            }
        }
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                isPanning = false;
            }
        }
        public void mouseClicked(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            Component c = canvas.getComponentAt(e.getPoint());
            if (isConnecting && connectStartProxy != null && !(c instanceof ProxyComponent)) {
                isConnecting = false;
                connectStartProxy = null;
                canvas.setConnectStartProxy(null);
                canvas.setMousePoint(null);
            }
            if (!(c instanceof ProxyComponent)) {
                unselectProxy();
            }
        }
    };

    private final MouseMotionListener canvasMouseMotionListener = new MouseMotionAdapter() {
        public void mouseDragged(MouseEvent e) {
            if (isPanning) {
                int dx = e.getX() - panStart.x;
                int dy = e.getY() - panStart.y;
                pan.translate(dx, dy);
                panStart = e.getPoint();
                updateProxyViewLocations();
            }
            if (isConnecting && connectStartProxy != null) {
                canvas.setMousePoint(e.getPoint());
            }
        }
        public void mouseMoved(MouseEvent e) {
            ConnectionLine nearest = canvas.getConnectionAt(e.getPoint());
            canvas.setHighlightedConnection(nearest);
            if (isConnecting && connectStartProxy != null) {
                canvas.setMousePoint(e.getPoint());
            }
        }
    };

    private void selectProxy(String id) {
        if (selectedProxyId != null) {
            ProxyController prev = proxyControllers.get(selectedProxyId);
            if (prev != null) prev.setSelected(false);
        }
        selectedProxyId = id;
        ProxyController curr = proxyControllers.get(id);
        if (curr != null) curr.setSelected(true);
        updateProxyBarVisibility();

        if (curr != null) {
            proxyBar.setDomainName(curr.getDomainName());
            proxyBar.setDescription(curr.getDescription());
            proxyBar.setShowInStreamsEnabled(curr.isShowParserEnabled());
        } else {
            proxyBar.setDomainName("");
            proxyBar.setDescription("");
            proxyBar.setShowInStreamsEnabled(false);
        }

        // Initialize UI components only once
        if (cachedParsedRequestPanel == null) {
            cachedParsedRequestPanel = new HttpMultiEditorPanel(
                    "Parsed Request",
                    HTTPRaiderExtension.API.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY)
            );
        }
        
        if (cachedStreamsBox == null) {
            cachedStreamsBox = new StreamComboBox<>("Request from Stream");
            
            // Set up the combo box listener only once
            reqEditor.setComponent(cachedStreamsBox, e -> {
                selectedReqId = cachedStreamsBox.getSelectedIndex();
                reqEditor.setBytes(cachedStreamsBox.getSelectedValue());

                if (selectedProxyId != null) {
                    ProxyController controller = proxyControllers.get(selectedProxyId);
                    if (controller != null) {
                        List<List<byte[]>> panelGroups = ParserChainRunner.parseFinalGroupsForPanel(
                                controller.getModel(),
                                reqEditor.getBytes(),
                                this
                        );
                        cachedParsedRequestPanel.addAll(panelGroups);
                    }
                }
            });
        }
        
        // Refresh combo box items without triggering selection change
        boolean isFirstTime = cachedStreamsBox.getComboBox().getItemCount() == 0;
        
        // Store current editor content before updating combo box
        byte[] currentContent = reqEditor.getBytes();
        
        // Temporarily remove listeners to prevent triggering during update
        ActionListener[] listeners = cachedStreamsBox.getComboBox().getActionListeners();
        for (ActionListener l : listeners) {
            cachedStreamsBox.getComboBox().removeActionListener(l);
        }
        
        cachedStreamsBox.getComboBox().removeAllItems();
        String domainName = proxyBar.getDomainName();
        if (domainName == null) domainName = "localhost";
        cachedStreamsBox.addItem(new ComboItem<>("Base Req", ("GET / HTTP/1.1\r\nHost: " + domainName + "\r\nContent-Length: 10\r\n\r\n0123456789").getBytes()));
        for (StreamController sc : streamControllers) {
            cachedStreamsBox.addItem(new ComboItem<>(sc.getName(), sc.getRequest()));
        }
        cachedStreamsBox.setSelectedIndex(selectedReqId);
        
        // Re-add listeners
        for (ActionListener l : listeners) {
            cachedStreamsBox.getComboBox().addActionListener(l);
        }
        
        // Restore editor content if it existed, or set initial content if first time
        if (isFirstTime || currentContent == null || currentContent.length == 0) {
            reqEditor.setBytes(cachedStreamsBox.getSelectedValue());
        } else {
            reqEditor.setBytes(currentContent);
        }
        
        // Clear parsed results
        cachedParsedRequestPanel.addAll(new ArrayList<>());

        cachedParsedRequestPanel.getEditorPanel().setComponent(new ActionButton("Test"), e -> {
            if (selectedProxyId != null) {
                ProxyController controller = proxyControllers.get(selectedProxyId);
                if (controller != null) {
                    List<List<byte[]>> panelGroups = ParserChainRunner.parseFinalGroupsForPanel(
                            controller.getModel(),
                            reqEditor.getBytes(),
                            this
                    );
                    cachedParsedRequestPanel.addAll(panelGroups);
                }
            }
        });

        view.showHttpEditors(reqEditor, cachedParsedRequestPanel);
        view.resetSplitPanes();

        if (selectedProxyId != null) {
            ProxyController controller = proxyControllers.get(selectedProxyId);
            if (controller != null) {
                List<List<byte[]>> panelGroups = ParserChainRunner.parseFinalGroupsForPanel(
                        controller.getModel(),
                        reqEditor.getBytes(),
                        this
                );
                cachedParsedRequestPanel.addAll(panelGroups);
            }
        }
    }


    public List<ProxyModel> sortByDistanceToClient(Set<ProxyModel> proxies) {
        if (proxies == null || proxies.isEmpty()) return Collections.emptyList();

        List<ProxyModel> list = new ArrayList<>(proxies);
        list.sort((p1, p2) -> {
            List<ProxyModel> path1 = getConnectionPathToClient(p1.getId());
            List<ProxyModel> path2 = getConnectionPathToClient(p2.getId());
            int d1 = (path1 == null) ? Integer.MAX_VALUE : path1.size();
            int d2 = (path2 == null) ? Integer.MAX_VALUE : path2.size();
            return Integer.compare(d1, d2);
        });
        return list;
    }


    public List<ProxyModel> getConnectionPathToClient(String proxyId) {
        if (proxyId == null) return null;
        String clientId = null;
        for (ProxyModel p : model.getProxies()) {
            if (p.isClient()) {
                clientId = p.getId();
                break;
            }
        }
        if (clientId == null || proxyId.equals(clientId)) return null;
        Map<String, Set<String>> adj = new HashMap<>();
        for (ProxyModel p : model.getProxies()) {
            adj.put(p.getId(), new HashSet<>());
        }
        for (ConnectionModel c : model.getConnections()) {
            adj.get(c.getFromId()).add(c.getToId());
            adj.get(c.getToId()).add(c.getFromId());
        }
        Map<String, String> parent = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(clientId);
        visited.add(clientId);
        boolean found = false;
        while (!queue.isEmpty() && !found) {
            String current = queue.poll();
            for (String neighbor : adj.get(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                    if (neighbor.equals(proxyId)) {
                        found = true;
                        break;
                    }
                    queue.add(neighbor);
                }
            }
        }
        if (!found) return null;
        List<ProxyModel> path = new ArrayList<>();
        String curr = proxyId;
        while (parent.containsKey(curr)) {
            String prev = parent.get(curr);
            if (prev.equals(clientId)) break;
            path.add(model.getProxy(prev));
            curr = prev;
        }
        Collections.reverse(path);
        return path;
    }

    private void unselectProxy() {
        if (selectedProxyId != null) {
            ProxyController prev = proxyControllers.get(selectedProxyId);
            if (prev != null) prev.setSelected(false);
        }
        selectedProxyId = null;
        updateProxyBarVisibility();
        proxyBar.setDomainName("");
        proxyBar.setDescription("");
        proxyBar.setShowInStreamsEnabled(false);
        view.hideHttpEditors();
    }

    private void updateProxyBarVisibility() {
        view.setProxyBarVisible(selectedProxyId != null);
    }
    
    private Point findFreePosition() {
        // Find a free position that doesn't overlap with existing proxies
        int baseX = 350;
        int baseY = 250;
        int offsetX = 150;
        int offsetY = 100;
        
        Set<Point> occupiedPositions = new HashSet<>(positions.values());
        
        // Try different positions in a grid pattern
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Point candidate = new Point(baseX + col * offsetX, baseY + row * offsetY);
                boolean isFree = true;
                
                // Check if this position is too close to any existing proxy
                for (Point occupied : occupiedPositions) {
                    if (Math.abs(candidate.x - occupied.x) < 100 && 
                        Math.abs(candidate.y - occupied.y) < 80) {
                        isFree = false;
                        break;
                    }
                }
                
                if (isFree) {
                    return candidate;
                }
            }
        }
        
        // If no free position found, use a random offset
        return new Point(baseX + (int)(Math.random() * 500), 
                        baseY + (int)(Math.random() * 400));
    }

    private void showConnectionMenu(ConnectionLine cv, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem del = new JMenuItem("Delete");
        del.addActionListener(e -> {
            ConnectionModel cm = new ConnectionModel(
                    cv.getFrom().getId(), cv.getTo().getId()
            );
            model.removeConnection(cm.getFromId(), cm.getToId());
            canvas.removeConnectionView(cv);
            if (selectedProxyId != null){
                selectProxy(selectedProxyId);
            }
        });
        menu.add(del);
        menu.show(canvas, x, y);
    }

    private void showProxyMenu(ProxyComponent pv, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        if (!pv.isClient()) {
            JMenuItem del = new JMenuItem("Delete");
            del.addActionListener(e -> {
                String proxyId = pv.getId();
                
                if (selectedProxyId != null && selectedProxyId.equals(proxyId)) {
                    unselectProxy();
                }
                
                model.removeProxy(proxyId);
                positions.remove(proxyId);
                proxyControllers.remove(proxyId);
                
                reloadAll(false);
                
                for (StreamController streamController : streamControllers){
                    streamController.resetView();
                }
            });
            menu.add(del);
        }
        JMenuItem connect = new JMenuItem("Connect");
        connect.addActionListener(e -> {
            isConnecting = true;
            connectStartProxy = pv;
            canvas.setConnectStartProxy(pv);
            canvas.setMousePoint(null);
        });
        menu.add(connect);
        menu.show(
                canvas,
                x - canvas.getLocationOnScreen().x,
                y - canvas.getLocationOnScreen().y
        );
    }

    private void showBackgroundMenu(int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem addProxy = new JMenuItem("Add Proxy");
        addProxy.addActionListener(e -> {
            ProxyModel proxy = new ProxyModel(
                    "Proxy " + model.getProxies().size()
            );
            model.addProxy(proxy);
            ProxyComponent proxyComponent = new ProxyComponent(proxy.getId(), proxy.isClient(), proxy.isShowParser());
            ProxyController pc = new ProxyController(proxy, proxyComponent);
            proxyControllers.put(proxy.getId(), pc);
            
            Point pos = new Point(x - pan.x, y - pan.y);
            positions.put(proxy.getId(), pos);
            proxyComponent.setSize(ICON_WIDTH, ICON_HEIGHT);
            Point displayPos = new Point(pos.x + pan.x, pos.y + pan.y);
            proxyComponent.setLocation(displayPos.x, displayPos.y);
            canvas.addProxyView(proxy.getId(), proxyComponent, displayPos);
            installProxyListeners(pc);
            updateProxyBarVisibility();
        });
        menu.add(addProxy);
        
        JMenuItem importProxy = new JMenuItem("Import Proxy");
        importProxy.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Import Proxy Configuration");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json"));
            
            if (fileChooser.showOpenDialog(view) == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                
                try {
                    ProxyModel importedProxy = ProxyExporter.importProxy(file);
                    
                    // Add the imported proxy to the network at the menu location
                    Point pos = new Point(x - pan.x, y - pan.y);
                    model.addProxy(importedProxy);
                    
                    ProxyComponent proxyComponent = new ProxyComponent(importedProxy.getId(), importedProxy.isClient(), importedProxy.isShowParser());
                    ProxyController pc = new ProxyController(importedProxy, proxyComponent);
                    proxyControllers.put(importedProxy.getId(), pc);
                    
                    // Store position and set component location
                    positions.put(importedProxy.getId(), pos);
                    proxyComponent.setSize(ICON_WIDTH, ICON_HEIGHT);
                    Point displayPos = new Point(pos.x + pan.x, pos.y + pan.y);
                    proxyComponent.setLocation(displayPos.x, displayPos.y);
                    
                    canvas.addProxyView(importedProxy.getId(), proxyComponent, displayPos);
                    installProxyListeners(pc);
                    
                    // Select the newly imported proxy
                    selectProxy(importedProxy.getId());
                    
                    JOptionPane.showMessageDialog(view, "Proxy configuration imported successfully!", 
                        "Import Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, "Error importing proxy: " + ex.getMessage(), 
                        "Import Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        menu.add(importProxy);
        menu.show(canvas, x, y);
    }

    public List<ProxyModel> getDirectConnections(String proxyId) {
        Set<String> directIds = new HashSet<>();
        for (ConnectionModel c : model.getConnections()) {
            if (c.getFromId().equals(proxyId)) {
                directIds.add(c.getToId());
            } else if (c.getToId().equals(proxyId)) {
                directIds.add(c.getFromId());
            }
        }
        List<ProxyModel> result = new ArrayList<>();
        for (String id : directIds) {
            ProxyModel p = model.getProxy(id);
            if (p != null) result.add(p);
        }
        return result;
    }

    // Returns true if there exists a path from targetId to the client that passes through viaId
    public boolean hasPathThroughProxyToClient(String targetId, String viaId) {
        if (targetId == null || viaId == null) return false;
        String clientId = null;
        for (ProxyModel p : model.getProxies()) {
            if (p.isClient()) {
                clientId = p.getId();
                break;
            }
        }
        if (clientId == null || targetId.equals(clientId)) return false;
        // Standard BFS from targetId to client, keeping track if viaId is traversed
        Set<String> visited = new HashSet<>();
        Queue<List<String>> queue = new LinkedList<>();
        List<String> start = new ArrayList<>();
        start.add(targetId);
        queue.add(start);
        visited.add(targetId);
        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            String current = path.get(path.size() - 1);
            if (current.equals(clientId)) {
                // Check if viaId is in the path (excluding start or end)
                for (int i = 1; i < path.size() - 1; i++) {
                    if (path.get(i).equals(viaId)) return true;
                }
            }
            for (ProxyModel neighbor : getDirectConnections(current)) {
                if (!visited.contains(neighbor.getId())) {
                    visited.add(neighbor.getId());
                    List<String> newPath = new ArrayList<>(path);
                    newPath.add(neighbor.getId());
                    queue.add(newPath);
                }
            }
        }
        return false;
    }


    public ProxyController getProxyController(String proxyId) {
        return proxyControllers.get(proxyId);
    }

    public void save() {}

    public void addConnectionQuick(String fromId, String toId) {
        model.addConnection(fromId, toId);
        ProxyComponent from = canvas.getProxyView(fromId);
        ProxyComponent to = canvas.getProxyView(toId);
        if (from != null && to != null) {
            canvas.addConnectionView(new ConnectionLine(from, to));
        }
    }
    
    public void addProxyQuick(ProxyModel proxy) {
        model.addProxy(proxy);
        ProxyComponent proxyComponent = new ProxyComponent(proxy.getId(), proxy.isClient(), proxy.isShowParser());
        ProxyController pc = new ProxyController(proxy, proxyComponent);
        proxyControllers.put(proxy.getId(), pc);
        
        Point pos = new Point(350, 250);
        positions.put(proxy.getId(), pos);
        proxyComponent.setSize(ICON_WIDTH, ICON_HEIGHT);
        Point displayPos = new Point(pos.x + pan.x, pos.y + pan.y);
        proxyComponent.setLocation(displayPos.x, displayPos.y);
        canvas.addProxyView(proxy.getId(), proxyComponent, displayPos);
        installProxyListeners(pc);
    }

    public void load() {
        reloadAll(true);
    }
    
    public void refreshView() {
        reloadAll(true);
    }

    public NetworkPanel getView() {
        return view;
    }

    public NetworkModel getModel() {
        return model;
    }
}
