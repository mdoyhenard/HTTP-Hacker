package httpraider.controller;

import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;
import extension.HTTPRaiderExtension;
import httpraider.model.network.ConnectionModel;
import httpraider.model.network.NetworkModel;
import httpraider.model.network.ProxyModel;
import httpraider.view.components.ConnectionLine;
import httpraider.view.components.ProxyComponent;
import httpraider.view.panels.*;
import httpraider.view.menuBars.NetworkBar;
import httpraider.view.menuBars.ProxyBar;

import javax.swing.*;
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
    private final Map<String, Point> positions;
    private Point pan;
    private boolean isPanning;
    private Point panStart;
    private Component pressedComponent;
    private ProxyComponent draggingProxy;
    private Point dragOffset;
    private ProxyComponent connectStartProxy;
    private boolean isConnecting;
    private String selectedProxyId;
    private boolean useForwardedRequest;

    public static final int ICON_WIDTH = 47;
    public static final int ICON_HEIGHT = 65;

    public NetworkController(NetworkModel model, NetworkPanel view) {
        this.model = model;
        this.view = view;
        this.canvas = view.getNetworkCanvas();
        this.networkBar = view.getNetworkBar();
        this.proxyBar = view.getProxyBar();
        this.positions = new HashMap<>();
        this.pan = new Point(0, 0);
        this.isPanning = false;
        this.panStart = null;
        this.pressedComponent = null;
        this.draggingProxy = null;
        this.dragOffset = null;
        this.connectStartProxy = null;
        this.isConnecting = false;
        this.selectedProxyId = null;

        canvas.setPan(pan);
        reloadAll(true);

        canvas.addCanvasMouseListener(canvasMouseListener);
        canvas.addCanvasMouseMotionListener(canvasMouseMotionListener);

        networkBar.setAutoLayoutActionListener(e -> reloadAll(true));
        networkBar.setDiscoverActionListener(e -> {});
        view.setProxyBarVisible(false);
        installBarListeners();
    }

    private void reloadAll(boolean layoutOnLoad) {
        canvas.clear();
        List<ProxyModel> proxies = new ArrayList<>(model.getProxies());
        if (layoutOnLoad) {
            positions.clear();
            positions.putAll(generateLayout(proxies));
        }
        for (ProxyModel proxy : proxies) {
            ProxyComponent pv = new ProxyComponent(proxy.getId(), proxy.isClient());
            Point pos = positions.get(proxy.getId());
            if (pos == null) {
                pos = new Point(350, 250);
                positions.put(proxy.getId(), pos);
            }
            pv.setSize(ICON_WIDTH, ICON_HEIGHT);
            Point displayPos = new Point(pos.x + pan.x, pos.y + pan.y);
            pv.setSelected(proxy.getId().equals(selectedProxyId));
            canvas.addProxyView(proxy.getId(), pv, displayPos);
            installProxyListeners(pv, proxy);
        }
        for (ConnectionModel c : model.getConnections()) {
            ProxyComponent from = canvas.getProxyView(c.getFromId());
            ProxyComponent to   = canvas.getProxyView(c.getToId());
            if (from != null && to != null) {
                canvas.addConnectionView(new ConnectionLine(from, to));
            }
        }
        canvas.setConnectStartProxy(isConnecting ? connectStartProxy : null);
        canvas.setMousePoint(null);
        canvas.setHighlightedConnection(null);
        canvas.repaint();
        updateProxyBarVisibility();
    }

    /**
     * Layout algorithm:
     * 1) BFS from client to assign each proxy a base 'depth' and build a symmetric tree for y‐indices.
     * 2) Place any unconnected proxies to the right, picking the first free row.
     * 3) Find any proxy D that sits at the same depth as two neighbors B,C which themselves are connected:
     *    bump D one column to the right (and move to the nearest free row if needed).
     * 4) Final (x,y) = (baseX + depth*gapX, baseY + yIndex*gapY).
     */
    private Map<String, Point> generateLayout(List<ProxyModel> proxies) {
        int baseX = 50, gapX = 220, gapY = 120, baseY = gapY;

        // 1) Find the client
        String clientId = null;
        for (ProxyModel p : proxies) {
            if (p.isClient()) { clientId = p.getId(); break; }
        }
        if (clientId == null) return new HashMap<>();

        // 2) Build full adjacency for connection checks
        Map<String, Set<String>> adj = new HashMap<>();
        for (ProxyModel p : proxies) adj.put(p.getId(), new HashSet<>());
        for (ConnectionModel c : model.getConnections()) {
            adj.get(c.getFromId()).add(c.getToId());
            adj.get(c.getToId()).add(c.getFromId());
        }

        // 3) BFS from client → parent/children + base level
        Map<String, List<String>> children = new HashMap<>();
        Map<String, Integer> level           = new HashMap<>();
        Set<String> visited                   = new HashSet<>();
        Queue<String> queue                   = new LinkedList<>();
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

        // 4) Symmetric‐tree Y indices for connected nodes
        Map<String, Integer> nodeY = new HashMap<>();
        int[] leafCounter          = {0};
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
                    last  = Math.max(last, y);
                }
                int mid = (first + last) / 2;
                nodeY.put(id, mid);
                return mid;
            }
        }
        new Positioner().assign(clientId);

        // 5) Record each node’s depth and initial yIndex, track used rows per depth
        Map<String, Integer> depthOf  = new HashMap<>();
        Map<String, Integer> yIndexOf = new HashMap<>();
        Map<Integer, Set<Integer>> usedY = new HashMap<>();
        for (ProxyModel p : proxies) {
            String id = p.getId();
            if (nodeY.containsKey(id)) {
                int d    = level.getOrDefault(id, 0);
                int yIdx = nodeY.get(id);
                depthOf.put(id, d);
                yIndexOf.put(id, yIdx);
                usedY.computeIfAbsent(d, k -> new HashSet<>()).add(yIdx);
            }
        }

        // 6) Any proxies not in the client‐tree get placed one column further for each
        int maxBaseDepth = level.values().stream()
                .max(Integer::compareTo)
                .orElse(0);
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

        // 7) Build a quick lookup of direct connections
        Map<String, Set<String>> direct = new HashMap<>();
        for (ProxyModel p : proxies) direct.put(p.getId(), new HashSet<>());
        for (ConnectionModel c : model.getConnections()) {
            direct.get(c.getFromId()).add(c.getToId());
            direct.get(c.getToId()).add(c.getFromId());
        }

        // 8) TRIANGLE CASE — bump any node D that sits at the same depth as two neighbors B,C
        //    which themselves are connected to each other.
        List<String> toPush = new ArrayList<>();
        for (String id : depthOf.keySet()) {
            Integer baseLvl = level.get(id);
            if (baseLvl == null) continue;
            // collect same‐level neighbors
            List<String> sameLvl = new ArrayList<>();
            for (String nbr : adj.get(id)) {
                if (Objects.equals(level.get(nbr), baseLvl)) {
                    sameLvl.add(nbr);
                }
            }
            if (sameLvl.size() < 2) continue;
            // if any pair among them is connected, we have a triangle
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

        // 9) Perform the bump: move each D → depth+1, then find a free row if needed
        for (String id : toPush) {
            int oldD = depthOf.get(id);
            int newD = oldD + 1;
            int yIdx = yIndexOf.get(id);

            // free up old slot
            usedY.get(oldD).remove(yIdx);

            // pick new depth & row
            depthOf.put(id, newD);
            Set<Integer> usedNew = usedY.computeIfAbsent(newD, k -> new HashSet<>());
            if (usedNew.contains(yIdx)) {
                // find nearest free yIdx
                for (int delta = 1; delta < proxies.size(); delta++) {
                    int low  = yIdx - delta;
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

        // 10) Build final positions
        Map<String, Point> result = new HashMap<>();
        for (String id : depthOf.keySet()) {
            int d    = depthOf.get(id);
            int yIdx = yIndexOf.get(id);
            int x    = baseX + d    * gapX;
            int y    = baseY + yIdx * gapY;
            result.put(id, new Point(x, y));
        }

        return result;
    }

    private void updateProxyViewLocations() {
        for (ProxyModel p : model.getProxies()) {
            ProxyComponent pv = canvas.getProxyView(p.getId());
            Point pos    = positions.get(p.getId());
            if (pv != null && pos != null) {
                pv.setLocation(pos.x + pan.x, pos.y + pan.y);
            }
        }
        canvas.repaint();
    }

    private void installBarListeners(){
        // Description ↔ model
        proxyBar.addDescriptionListener(new DocumentListener() {
            private void update() {
                if (selectedProxyId != null) {
                    model.getProxy(selectedProxyId).setDescription(proxyBar.getDescription());
                }
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

// BasePath ↔ model
        proxyBar.addBasePathListener(new DocumentListener() {
            private void update() {
                if (selectedProxyId != null) {
                    model.getProxy(selectedProxyId).setBasePath(proxyBar.getBasePath());
                }
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

// Parsing-code button
        proxyBar.addParsingCodeListener(e -> {
            if (selectedProxyId == null) return;
            ProxyModel pm = model.getProxy(selectedProxyId);
            JTextArea editor = new JTextArea(pm.getParsingCode(), 20, 60);
            JScrollPane pane = new JScrollPane(editor);
            int opt = JOptionPane.showConfirmDialog(
                    view, pane, "Edit parsing code",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
            );
            if (opt == JOptionPane.OK_OPTION) {
                pm.setParsingCode(editor.getText());
            }
        });

// Forwarding-code button
        proxyBar.addForwardingCodeListener(e -> {
            if (selectedProxyId == null) return;
            ProxyModel pm = model.getProxy(selectedProxyId);
            JTextArea editor = new JTextArea(pm.getForwardingCode(), 20, 60);
            JScrollPane pane = new JScrollPane(editor);
            int opt = JOptionPane.showConfirmDialog(
                    view, pane, "Edit forwarding code",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
            );
            if (opt == JOptionPane.OK_OPTION) {
                pm.setForwardingCode(editor.getText());
            }
        });

// Show-in-streams toggle
        proxyBar.addShowInStreamsListener(e -> {
            // listener stub: proxy toggled; behavior to implement later
            boolean on = ((JToggleButton)e.getSource()).isSelected();
            System.out.println("Show-in-streams for " + selectedProxyId + ": " + on);
        });

        // DomainName ↔ model
        proxyBar.addDomainNameListener(new DocumentListener() {
            private void update() {
                if (selectedProxyId != null) {
                    model.getProxy(selectedProxyId).setDomainName(proxyBar.getDomainName());
                }
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });


    }

    private void installProxyListeners(ProxyComponent pv, ProxyModel pm) {
        pv.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showProxyMenu(pv, e.getXOnScreen(), e.getYOnScreen());
                    return;
                }
                pressedComponent = pv;
                if (SwingUtilities.isLeftMouseButton(e)) {
                    draggingProxy = pv;
                    dragOffset    = e.getPoint();
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) return;
                draggingProxy = null;
                dragOffset    = null;
            }
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (isConnecting
                        && connectStartProxy != null
                        && pv != connectStartProxy) {
                    model.addConnection(connectStartProxy.getId(), pv.getId());
                    reloadAll(false);
                    isConnecting      = false;
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
        pv.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (draggingProxy != null) {
                    int nx = draggingProxy.getX() + e.getX() - dragOffset.x;
                    int ny = draggingProxy.getY() + e.getY() - dragOffset.y;
                    positions.put(
                            draggingProxy.getId(),
                            new Point(nx - pan.x, ny - pan.y)
                    );
                    draggingProxy.setLocation(nx, ny);
                    canvas.repaint();
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
                panStart  = e.getPoint();
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
                isConnecting      = false;
                connectStartProxy = null;
                canvas.setConnectStartProxy(null);
                canvas.setMousePoint(null);
                canvas.repaint();
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
                canvas.repaint();
            }
        }
        public void mouseMoved(MouseEvent e) {
            ConnectionLine nearest = canvas.getConnectionAt(e.getPoint());
            canvas.setHighlightedConnection(nearest);
            if (isConnecting && connectStartProxy != null) {
                canvas.setMousePoint(e.getPoint());
                canvas.repaint();
            }
        }
    };

    private void selectProxy(String id) {
        if (selectedProxyId != null && canvas.getProxyView(selectedProxyId) != null) {
            canvas.getProxyView(selectedProxyId).setSelected(false);
        }
        selectedProxyId = id;
        if (canvas.getProxyView(id) != null) {
            canvas.getProxyView(id).setSelected(true);
        }
        updateProxyBarVisibility();

        ProxyModel pm = model.getProxy(id);
        List<ProxyModel> proxiesToClient = getConnectionPathToClient(id);

        proxyBar.setDomainName(pm.getDomainName());
        proxyBar.setDescription(pm.getDescription());
        proxyBar.setBasePath(pm.getBasePath());

        HTTPEditorPanel<HttpRequestEditor> reqEditor =
                new HTTPEditorPanel<>("Base Request",
                        HTTPRaiderExtension.API.userInterface().createHttpRequestEditor());
        HTTPEditorPanel<WebSocketMessageEditor> parsedReq =
                new HTTPEditorPanel<>("Parsed Request",
                        HTTPRaiderExtension.API.userInterface().createWebSocketMessageEditor(EditorOptions.READ_ONLY));
        useForwardedRequest = false;

        if (proxiesToClient != null && !proxiesToClient.isEmpty()) parsedReq.setSwitch("Use transformations", e -> {useForwardedRequest = !useForwardedRequest;});
        view.showHttpEditors(reqEditor, parsedReq);
    }

    private List<ProxyModel> getConnectionPathToClient(String proxyId) {
        if (proxyId == null) return null;
        // Find the client proxy ID
        String clientId = null;
        for (ProxyModel p : model.getProxies()) {
            if (p.isClient()) {
                clientId = p.getId();
                break;
            }
        }
        if (clientId == null || proxyId.equals(clientId)) return null;

        // Build adjacency map
        Map<String, Set<String>> adj = new HashMap<>();
        for (ProxyModel p : model.getProxies()) {
            adj.put(p.getId(), new HashSet<>());
        }
        for (ConnectionModel c : model.getConnections()) {
            adj.get(c.getFromId()).add(c.getToId());
            adj.get(c.getToId()).add(c.getFromId());
        }

        // BFS from client to find path
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

        // Reconstruct path
        List<ProxyModel> path = new ArrayList<>();
        String curr = proxyId;
        while (parent.containsKey(curr)) {
            String prev = parent.get(curr);
            if (prev.equals(clientId)) break;
            path.add(0, model.getProxy(prev));
            curr = prev;
        }
        return path;
    }


    private void unselectProxy() {
        if (selectedProxyId != null && canvas.getProxyView(selectedProxyId) != null) {
            canvas.getProxyView(selectedProxyId).setSelected(false);
        }
        selectedProxyId = null;
        updateProxyBarVisibility();

        proxyBar.setDomainName("");
        proxyBar.setDescription("");
        proxyBar.setBasePath("");

        view.hideHttpEditors();
    }

    private void updateProxyBarVisibility() {
        view.setProxyBarVisible(selectedProxyId != null);
    }

    private void showConnectionMenu(ConnectionLine cv, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem del = new JMenuItem("Delete");
        del.addActionListener(e -> {
            ConnectionModel cm = new ConnectionModel(
                    cv.getFrom().getId(), cv.getTo().getId()
            );
            model.removeConnection(cm.getFromId(), cm.getToId());
            reloadAll(false);
        });
        menu.add(del);
        menu.show(canvas, x, y);
    }

    private void showProxyMenu(ProxyComponent pv, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        if (!pv.isClient()) {
            JMenuItem del = new JMenuItem("Delete");
            del.addActionListener(e -> {
                model.removeProxy(pv.getId());
                positions.remove(pv.getId());
                reloadAll(false);
            });
            menu.add(del);
        }
        JMenuItem connect = new JMenuItem("Connect");
        connect.addActionListener(e -> {
            isConnecting      = true;
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
            positions.put(
                    proxy.getId(),
                    new Point(x - pan.x, y - pan.y)
            );
            reloadAll(false);
        });
        menu.add(addProxy);
        menu.show(canvas, x, y);
    }

    public void save() {}

    public void load() {
        reloadAll(true);
    }

    public NetworkPanel getView() {
        return view;
    }

    public NetworkModel getModel() {
        return model;
    }
}
