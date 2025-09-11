package httpraider.view.panels;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import httpraider.view.components.helpers.HttpRequestEditorHighlighter;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class HttpMultiEditorPanel extends JPanel {
    public static final int LIST_WIDTH = 70;
    public static final Color ALL_GROUP_COLOR = new Color(204, 227, 188, 216);
    public static final Color ALL_GROUP_COLOR_SEL = new Color(213, 236, 197, 94);
    public static final Color LIST_GROUP_COLOR = new Color(235, 235, 235, 102);
    public static final Color LIST_SELECTION_COLOR = new Color(120, 180, 255, 76);
    public static final Color LIST_GROUP_HEADER_COLOR = new Color(218, 218, 218, 202); // Change as desired
    public static final Color LIST_DIVIDER_COLOR = UIManager.getColor("List.background");

    private final HttpEditorPanel httpEditorPanel;
    private final JList<ListEntry> requestList;
    private final DefaultListModel<ListEntry> listModel;
    private final JScrollPane listScrollPane;
    private final Map<Integer, List<byte[]>> groups = new LinkedHashMap<>();
    private int reqCounter = 1;
    private int nextGroupId = 1;
    private HttpRequestEditorHighlighter highlighter;

    public HttpMultiEditorPanel(String text, HttpRequestEditor editor) {
        setLayout(new BorderLayout());
        if (editor.getRequest() == null) editor.setRequest(HttpRequest.httpRequest(""));
        this.httpEditorPanel = new HttpEditorPanel(text, editor);
        this.highlighter = new HttpRequestEditorHighlighter(editor);

        listModel = new DefaultListModel<>();
        requestList = new JList<>(listModel);
        requestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestList.setCellRenderer(new GroupedRequestListCellRenderer());
        requestList.setFixedCellHeight(-1);

        listScrollPane = new JScrollPane(requestList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScrollPane.setPreferredSize(new Dimension(LIST_WIDTH, 400));
        listScrollPane.setMaximumSize(new Dimension(LIST_WIDTH, Integer.MAX_VALUE));
        listScrollPane.setMinimumSize(new Dimension(LIST_WIDTH, 0));
        listScrollPane.setBorder(new MatteBorder(0, 1, 0, 1, Color.LIGHT_GRAY));

        requestList.addListSelectionListener(e -> {
            int idx = requestList.getSelectedIndex();
            if (idx >= 0) {
                ListEntry entry = listModel.get(idx);
                if (entry.type == ListEntryType.ALL) {
                    showAllConcatenated();
                } else if (entry.type == ListEntryType.REQUEST) {
                    httpEditorPanel.setBytes(entry.data);
                    highlighter.clearHighlights();
                } else {
                    int dir = e.getValueIsAdjusting() ? 0 : (idx > 0 ? -1 : 1);
                    int newIdx = findNextRequest(idx, dir);
                    if (newIdx != -1 && newIdx != idx) {
                        requestList.setSelectedIndex(newIdx);
                    }
                }
            }
        });

        requestList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int idx = requestList.getSelectedIndex();
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int prevIdx = findNextRequest(idx - 1, -1);
                    if (prevIdx != -1) {
                        requestList.setSelectedIndex(prevIdx);
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    int nextIdx = findNextRequest(idx + 1, 1);
                    if (nextIdx != -1) {
                        requestList.setSelectedIndex(nextIdx);
                        e.consume();
                    }
                }
            }
        });

        requestList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int idx = requestList.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    ListEntry entry = listModel.get(idx);
                    if (entry.type == ListEntryType.REQUEST) {
                        httpEditorPanel.setBytes(entry.data);
                        highlighter.clearHighlights();

                    }
                }
            }
        });

        refreshList();
    }

    private void showAllConcatenated() {
        List<byte[]> all = new ArrayList<>();
        for (List<byte[]> items : groups.values()) {
            if (items != null && !items.isEmpty()) {
                all.addAll(items);
            }
        }
        if (all.isEmpty()) {
            httpEditorPanel.setBytes(new byte[0]);
            highlighter.clearHighlights();
            return;
        }
        int totalLen = all.stream().mapToInt(a -> a.length).sum();
        byte[] concat = new byte[totalLen];
        int pos = 0;
        List<int[]> ranges = new ArrayList<>();
        for (byte[] part : all) {
            System.arraycopy(part, 0, concat, pos, part.length);
            ranges.add(new int[]{pos, pos + part.length});
            pos += part.length;
        }
        httpEditorPanel.setBytes(concat);

        SwingUtilities.invokeLater(() -> {
            highlighter = new HttpRequestEditorHighlighter((HttpRequestEditor) httpEditorPanel.getEditorPanel());
            highlighter.clearHighlights();
            Color[] highlightColors = {new Color(255,235,205, 121),
                    new Color(204,255,255, 124),
                    new Color(255,220,220, 119),
                    new Color(220,255,220, 124)};
            for (int i = 0; i < ranges.size(); i++) {
                int[] r = ranges.get(i);
                Color color = highlightColors[i % highlightColors.length];
                highlighter.addHighlightRange(r[0], r[1], color);
            }
        });
    }

    public int addGroup() {
        int groupId = nextGroupId++;
        groups.put(groupId, new ArrayList<>());
        refreshList();
        return groupId;
    }

    public int addGroup(List<byte[]> items) {
        int groupId = nextGroupId++;
        groups.put(groupId, new ArrayList<>(items));
        refreshList();
        return groupId;
    }

    public void addAll(List<List<byte[]>> newGroups) {
        groups.clear();
        nextGroupId = 1;
        for (List<byte[]> groupItems : newGroups) {
            int groupId = nextGroupId++;
            groups.put(groupId, new ArrayList<>(groupItems));
        }
        refreshList();
    }

    public void removeGroup(int groupId) {
        if (groups.containsKey(groupId)) {
            groups.remove(groupId);
            refreshList();
        }
    }

    public void addItemToGroup(int groupId, byte[] bytes) {
        if (!groups.containsKey(groupId)) {
            throw new IllegalArgumentException("Group id " + groupId + " does not exist.");
        }
        groups.get(groupId).add(bytes);
        refreshList();
    }

    public void clear() {
        groups.clear();
        nextGroupId = 1;
        reqCounter = 1;
        refreshList();
    }

    public HttpEditorPanel getEditorPanel() {
        return httpEditorPanel;
    }

    private int getTotalItemCount() {
        int total = 0;
        for (List<byte[]> l : groups.values()) {
            total += l.size();
        }
        return total;
    }

    private byte[] getSingleByteArray() {
        for (List<byte[]> l : groups.values()) {
            if (l != null && !l.isEmpty()) {
                return l.get(0);
            }
        }
        return new byte[0];
    }

    private void refreshList() {
        removeAll();
        listModel.clear();
        reqCounter = 1;
        int totalItems = getTotalItemCount();

        if (totalItems == 0) {
            httpEditorPanel.setBytes(new byte[0]);
            add(httpEditorPanel, BorderLayout.CENTER);
        } else {
            // Always show list view with "All" option, even for single request
            // Add "All" row at the top
            listModel.addElement(new ListEntry(ListEntryType.ALL, null, -1, 0));
            // Add small divider row after "All"
            listModel.addElement(new ListEntry(ListEntryType.DIVIDER, null, -1, 0));

            for (int groupId : groups.keySet()) {
                List<byte[]> items = groups.get(groupId);
                if (items != null && !items.isEmpty()) {
                    listModel.addElement(new ListEntry(ListEntryType.GROUP_HEADER, null, groupId, 0));
                    for (byte[] data : items) {
                        listModel.addElement(new ListEntry(ListEntryType.REQUEST, data, groupId, reqCounter));
                        reqCounter++;
                    }
                    listModel.addElement(new ListEntry(ListEntryType.DIVIDER, null, groupId, 0));
                }
            }
            add(listScrollPane, BorderLayout.WEST);
            add(httpEditorPanel, BorderLayout.CENTER);
            SwingUtilities.invokeLater(() -> {
                requestList.setSelectedIndex(0);
                showAllConcatenated();
            });
        }
        revalidate();
        repaint();
    }



    private int findNextRequest(int idx, int direction) {
        int n = listModel.size();
        if (direction < 0) {
            for (int i = idx; i >= 0; i--) {
                ListEntryType t = listModel.get(i).type;
                if (t == ListEntryType.REQUEST || t == ListEntryType.ALL) return i;
            }
        } else {
            for (int i = idx; i < n; i++) {
                ListEntryType t = listModel.get(i).type;
                if (t == ListEntryType.REQUEST || t == ListEntryType.ALL) return i;
            }
        }
        return -1;
    }

    private enum ListEntryType {
        ALL, GROUP_HEADER, REQUEST, DIVIDER
    }

    private static class ListEntry {
        final ListEntryType type;
        final byte[] data;
        final int groupId;
        final int reqNumber;

        ListEntry(ListEntryType type, byte[] data, int groupId, int reqNumber) {
            this.type = type;
            this.data = data;
            this.groupId = groupId;
            this.reqNumber = reqNumber;
        }
    }

    private static class GroupedRequestListCellRenderer extends JPanel implements ListCellRenderer<ListEntry> {
        private final JLabel label;

        public GroupedRequestListCellRenderer() {
            setLayout(new BorderLayout());
            label = new JLabel();
            label.setOpaque(false);
            add(label, BorderLayout.CENTER);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ListEntry> list, ListEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            removeAll();
            setOpaque(true);

            if (value.type == ListEntryType.ALL) {
                setPreferredSize(new Dimension(HttpMultiEditorPanel.LIST_WIDTH, 22));
                setBackground(isSelected ? HttpMultiEditorPanel.ALL_GROUP_COLOR : HttpMultiEditorPanel.ALL_GROUP_COLOR_SEL);
                label.setText("All");
                label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                add(label, BorderLayout.CENTER);
            }
            else if (value.type == ListEntryType.GROUP_HEADER) {
                setPreferredSize(new Dimension(HttpMultiEditorPanel.LIST_WIDTH, 22));
                setBackground(HttpMultiEditorPanel.LIST_GROUP_HEADER_COLOR);
                label.setText("Pipeline " + value.groupId);
                label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                add(label, BorderLayout.CENTER);
            } else if (value.type == ListEntryType.REQUEST) {
                setPreferredSize(new Dimension(HttpMultiEditorPanel.LIST_WIDTH, 22));
                setBackground(isSelected ? HttpMultiEditorPanel.LIST_SELECTION_COLOR : HttpMultiEditorPanel.LIST_GROUP_COLOR);
                label.setText("Req " + value.reqNumber);
                label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                add(label, BorderLayout.CENTER);
            } else if (value.type == ListEntryType.DIVIDER) {
                setPreferredSize(new Dimension(HttpMultiEditorPanel.LIST_WIDTH, 6));
                setBackground(HttpMultiEditorPanel.LIST_DIVIDER_COLOR);
                label.setText("");
                add(label, BorderLayout.CENTER);
            }
            return this;
        }
    }
}
