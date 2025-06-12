package httpraider.controller;

import httpraider.model.network.ConnectionModel;
import httpraider.model.network.NetworkModel;
import httpraider.model.network.ProxyModel;
import httpraider.view.panels.ConnectionView;
import httpraider.view.panels.NetworkPanel;
import httpraider.view.panels.ProxyView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class NetworkController {

    private final NetworkModel model;
    private final NetworkPanel view;
    private final Map<String, Point> positions;
    private double zoom;
    private Point pan;
    private boolean isPanning;
    private Point panStart;
    private ProxyView draggingProxy;
    private Point dragOffset;
    private ProxyView connectStartProxy;
    private boolean isConnecting;

    public static final int ICON_WIDTH = 47;
    public static final int ICON_HEIGHT = 65;

    public NetworkController(NetworkModel model, NetworkPanel view) {
        this.model = model;
        this.view = view;
        this.positions = new HashMap<>();
        this.zoom = 1.0;
        this.pan = new Point(0, 0);
        this.isPanning = false;
        this.panStart = null;
        this.draggingProxy = null;
        this.dragOffset = null;
        this.connectStartProxy = null;
        this.isConnecting = false;
        view.setZoom(zoom);
        view.setPan(pan);
        reloadAll(true);
        installGlobalListeners();
    }

    private boolean initialPanDone = false;

    private void reloadAll(boolean layoutOnLoad) {
        view.clear();
        List<ProxyModel> proxies = new ArrayList<>(model.getProxies());
        if (layoutOnLoad) {
            positions.clear();
            Map<String, Point> layoutPositions = generateLayout(proxies);
            positions.putAll(layoutPositions);
            // Apply initial upward pan ONLY ON FIRST LAYOUT
            if (!initialPanDone) {
                int upward = 180; // adjust this value to move higher/lower
                pan = new Point(0, -upward);
                view.setPan(pan);
                initialPanDone = true;
            }
        }
        // For all proxies, use logical + pan for actual display
        for (ProxyModel proxy : proxies) {
            ProxyView pv = new ProxyView(proxy.getId(), proxy.isClient());
            Point pos = positions.get(proxy.getId());
            if (pos == null) {
                pos = new Point(350, 250);
                positions.put(proxy.getId(), pos);
            }
            pv.setSize(ICON_WIDTH, ICON_HEIGHT);
            // Always add the pan offset here!
            Point displayPos = new Point(pos.x + pan.x, pos.y + pan.y);
            view.addProxyView(proxy.getId(), pv, displayPos);
            installProxyListeners(pv, proxy);
        }
        for (ConnectionModel c : model.getConnections()) {
            ProxyView from = view.getProxyView(c.getFromId());
            ProxyView to = view.getProxyView(c.getToId());
            if (from != null && to != null) {
                ConnectionView cv = new ConnectionView(from, to);
                view.addConnectionView(cv);
            }
        }
        view.setConnectStartProxy(isConnecting ? connectStartProxy : null);
        view.setMousePoint(null);
        view.repaint();
    }


    private Map<String, Point> generateLayout(List<ProxyModel> proxies) {
        int baseX = 50;
        int gapX = 220;
        int gapY = 120;
        int panelHeight = 900;

        String clientId = null;
        for (ProxyModel p : proxies) {
            if (p.isClient()) {
                clientId = p.getId();
                break;
            }
        }
        if (clientId == null) return new HashMap<>();

        Map<String, Set<String>> adj = new HashMap<>();
        for (ProxyModel p : proxies) adj.put(p.getId(), new HashSet<>());
        for (ConnectionModel c : model.getConnections()) {
            adj.get(c.getFromId()).add(c.getToId());
            adj.get(c.getToId()).add(c.getFromId());
        }

        Map<String, String> parent = new HashMap<>();
        Map<String, List<String>> children = new HashMap<>();
        Map<String, Integer> level = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(clientId);
        level.put(clientId, 0);
        visited.add(clientId);

        while (!queue.isEmpty()) {
            String id = queue.poll();
            for (String neigh : adj.get(id)) {
                if (!visited.contains(neigh)) {
                    parent.put(neigh, id);
                    children.computeIfAbsent(id, k -> new ArrayList<>()).add(neigh);
                    level.put(neigh, level.get(id) + 1);
                    queue.add(neigh);
                    visited.add(neigh);
                }
            }
        }

        // --- Tidy tree Y assignment: Y can be negative or positive (centered about root) ---
        Map<String, Integer> nodeY = new HashMap<>();
        int[] nextLeafY = {0};
        class Positioner {
            int assign(String id, int depth) {
                List<String> kids = children.getOrDefault(id, Collections.emptyList());
                if (kids.isEmpty()) {
                    nodeY.put(id, nextLeafY[0]);
                    return nextLeafY[0]++;
                } else {
                    int first = -1, last = -1;
                    for (String child : kids) {
                        int cy = assign(child, depth + 1);
                        if (first == -1) first = cy;
                        last = cy;
                    }
                    int y = (first + last) / 2;
                    nodeY.put(id, y);
                    return y;
                }
            }
        }
        Positioner pos = new Positioner();
        pos.assign(clientId, 0);

        // Find min and max Y (to center the tree)
        int minY = nodeY.values().stream().min(Integer::compareTo).orElse(0);
        int maxY = nodeY.values().stream().max(Integer::compareTo).orElse(0);
        int treeHeight = (maxY - minY) * gapY;
        int centerY = panelHeight / 2;
        int clientY = nodeY.get(clientId);
        int baseY = centerY - (clientY - minY) * gapY;

        Map<String, Point> result = new HashMap<>();
        for (ProxyModel p : proxies) {
            if (nodeY.containsKey(p.getId())) {
                int col = level.getOrDefault(p.getId(), 0);
                int x = baseX + col * gapX;
                int y = baseY + (nodeY.get(p.getId()) - minY) * gapY;
                result.put(p.getId(), new Point(x, y));
            }
        }

        // --- Unconnected nodes: compact, centered ---
        int maxDepth = level.values().stream().max(Integer::compareTo).orElse(0);
        int unconnectedX = baseX + gapX * (maxDepth + 1);
        List<ProxyModel> unconnected = new ArrayList<>();
        for (ProxyModel p : proxies) {
            if (!nodeY.containsKey(p.getId())) unconnected.add(p);
        }
        if (!unconnected.isEmpty()) {
            int n = unconnected.size();
            int unconnectedHeight = (n - 1) * gapY;
            int rootY = result.get(clientId).y;
            int startY = rootY - unconnectedHeight / 2;
            for (int i = 0; i < n; i++) {
                int y = startY + i * gapY;
                result.put(unconnected.get(i).getId(), new Point(unconnectedX, y));
            }
        }

        return result;
    }



    private void updateProxyViewLocations() {
        for (ProxyModel proxy : model.getProxies()) {
            ProxyView pv = view.getProxyView(proxy.getId());
            Point pos = positions.get(proxy.getId());
            if (pv != null && pos != null) {
                pv.setLocation(pos.x + pan.x, pos.y + pan.y);
            }
        }
        view.repaint();
    }

    private void installProxyListeners(ProxyView pv, ProxyModel modelProxy) {
        pv.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                pv.requestFocus();
                if (SwingUtilities.isLeftMouseButton(e)) {
                    draggingProxy = pv;
                    dragOffset = e.getPoint();
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    showProxyMenu(pv, e.getXOnScreen(), e.getYOnScreen());
                }
            }
            public void mouseReleased(MouseEvent e) {
                draggingProxy = null;
                dragOffset = null;
            }
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && isConnecting && connectStartProxy != null && pv != connectStartProxy) {
                    model.addConnection(connectStartProxy.getId(), pv.getId());
                    reloadAll(false);
                    isConnecting = false;
                    connectStartProxy = null;
                    view.setConnectStartProxy(null);
                    view.setMousePoint(null);
                    view.repaint();
                }
            }
        });
        pv.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (draggingProxy != null) {
                    int nx = draggingProxy.getX() + e.getX() - dragOffset.x;
                    int ny = draggingProxy.getY() + e.getY() - dragOffset.y;
                    Point logical = new Point(nx - pan.x, ny - pan.y);
                    draggingProxy.setLocation(nx, ny);
                    positions.put(draggingProxy.getId(), logical);
                    view.repaint();
                }
            }
        });
    }

    private void installGlobalListeners() {
        view.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                view.requestFocus();
                Component c = view.getComponentAt(e.getPoint());
                if (SwingUtilities.isLeftMouseButton(e) && !(c instanceof ProxyView)) {
                    isPanning = true;
                    panStart = e.getPoint();
                }
                if (SwingUtilities.isRightMouseButton(e) && !(c instanceof ProxyView)) {
                    ConnectionView highlighted = view.getHighlightedConnection();
                    if (highlighted != null) {
                        showConnectionMenu(highlighted, e.getX(), e.getY());
                    } else {
                        showBackgroundMenu(e.getX(), e.getY());
                    }
                }
                if (SwingUtilities.isLeftMouseButton(e) && isConnecting) {
                    if (!(c instanceof ProxyView)) {
                        isConnecting = false;
                        connectStartProxy = null;
                        view.setConnectStartProxy(null);
                        view.setMousePoint(null);
                        view.repaint();
                    }
                }
                if (SwingUtilities.isRightMouseButton(e) && isConnecting) {
                    isConnecting = false;
                    connectStartProxy = null;
                    view.setConnectStartProxy(null);
                    view.setMousePoint(null);
                    view.repaint();
                }
            }
            public void mouseReleased(MouseEvent e) {
                isPanning = false;
            }
        });
        view.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (isPanning) {
                    int dx = e.getX() - panStart.x;
                    int dy = e.getY() - panStart.y;
                    pan.translate(dx, dy);
                    panStart = e.getPoint();
                    updateProxyViewLocations();
                }
                if (isConnecting && connectStartProxy != null) {
                    view.setMousePoint(e.getPoint());
                }
            }
            public void mouseMoved(MouseEvent e) {
                if (isConnecting && connectStartProxy != null) {
                    view.setMousePoint(e.getPoint());
                }
            }
        });
        view.addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) {
                zoom = Math.min(zoom + 0.1, 2.0);
            } else {
                zoom = Math.max(zoom - 0.1, 0.4);
            }
            view.setZoom(zoom);
            view.repaint();
        });
    }


    private void showConnectionMenu(ConnectionView connectionView, int x, int y){
        JPopupMenu menu = new JPopupMenu();
        JMenuItem delete = new JMenuItem("Delete");
        delete.addActionListener(e -> {
            ConnectionModel cm = new ConnectionModel(connectionView.getFrom().getId(), connectionView.getTo().getId());
            model.removeConnection(cm.getFromId(), cm.getToId());
            reloadAll(false);
        });
        menu.add(delete);
        menu.show(view, x, y);
    }

    private void showProxyMenu(ProxyView pv, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem del = new JMenuItem("Delete");
        del.addActionListener(e -> {
            model.removeProxy(pv.getId());
            positions.remove(pv.getId());
            reloadAll(false);
        });
        menu.add(del);
        JMenuItem connect = new JMenuItem("Connect");
        connect.addActionListener(e -> {
            isConnecting = true;
            connectStartProxy = pv;
            view.setConnectStartProxy(pv);
            view.setMousePoint(null);
        });
        menu.add(connect);
        menu.show(view, x - view.getLocationOnScreen().x, y - view.getLocationOnScreen().y);
    }

    private void showBackgroundMenu(int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem addProxy = new JMenuItem("Add Proxy");
        addProxy.addActionListener(e -> {
            ProxyModel proxy = new ProxyModel("Proxy " + (model.getProxies().size()));
            model.addProxy(proxy);
            positions.put(proxy.getId(), new Point(x - pan.x, y - pan.y));
            reloadAll(false);
        });
        menu.add(addProxy);
        menu.show(view, x, y);
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
