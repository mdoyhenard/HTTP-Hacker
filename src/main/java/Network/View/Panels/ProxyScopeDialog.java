package Network.View.Panels;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.sitemap.SiteMap;
import burp.api.montoya.sitemap.SiteMapFilter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ProxyScopeDialog allows the user to filter site map entries by host and path prefix.
 * Available requests are displayed in a list; the user can add desired entries to the
 * "Chosen Requests" list using the >> button. A details area shows the full request/response.
 */
public class ProxyScopeDialog extends JDialog {

    private final MontoyaApi api;
    private final SiteMap siteMap;
    private JTextField hostField;
    private JTextField pathField;
    private JButton searchButton;
    private JList<SiteMapEntry> availableList;
    private DefaultListModel<SiteMapEntry> availableListModel;
    private JList<SiteMapEntry> chosenList;
    private DefaultListModel<SiteMapEntry> chosenListModel;
    private JButton addButton;
    private JButton removeButton;
    private JButton startButton;
    private JButton cancelButton;
    private JTextArea detailsTextArea;
    private boolean started = false;

    /**
     * Constructs the dialog.
     *
     * @param api     The Montoya API instance.
     * @param handler A handler to process the chosen entries when the user clicks Start.
     */
    public ProxyScopeDialog(MontoyaApi api, ProxyScopeHandler handler) {
        super((Frame) null, "Scope Configuration", true);
        this.api = api;
        this.siteMap = api.siteMap();
        initComponents();

        // When the Start button is pressed, call the handler with the chosen entries.
        startButton.addActionListener(e -> {
            started = true;
            List<SiteMapEntry> chosenEntries = getChosenEntries();
            setVisible(false);
            handler.handleSelectedEntries(extractHttpRequestResponses(chosenEntries));
        });
        cancelButton.addActionListener(e -> {
            started = false;
            setVisible(false);
        });

        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        // Top Panel: host and path filter fields plus search button.
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Host:"));
        hostField = new JTextField(20);
        topPanel.add(hostField);
        topPanel.add(new JLabel("Path Prefix:"));
        pathField = new JTextField(20);
        topPanel.add(pathField);
        searchButton = new JButton("Search");
        topPanel.add(searchButton);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center Panel: two lists (Available and Chosen) with add/remove buttons.
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Available Requests list.
        availableListModel = new DefaultListModel<>();
        availableList = new JList<>(availableListModel);
        availableList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane availableScroll = new JScrollPane(availableList);
        availableScroll.setPreferredSize(new Dimension(200, 300));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        centerPanel.add(new JLabel("Available Requests"), gbc);
        gbc.gridy = 1;
        centerPanel.add(availableScroll, gbc);

        // Add/Remove buttons.
        JPanel movePanel = new JPanel(new GridLayout(2, 1, 5, 5));
        addButton = new JButton(">>");
        removeButton = new JButton("<<");
        movePanel.add(addButton);
        movePanel.add(removeButton);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(movePanel, gbc);

        // Chosen Requests list.
        chosenListModel = new DefaultListModel<>();
        chosenList = new JList<>(chosenListModel);
        chosenList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane chosenScroll = new JScrollPane(chosenList);
        chosenScroll.setPreferredSize(new Dimension(200, 300));
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        centerPanel.add(new JLabel("Chosen Requests"), gbc);
        gbc.gridy = 1;
        centerPanel.add(chosenScroll, gbc);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom Panel: details area and Start/Cancel buttons.
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        detailsTextArea = new JTextArea(10, 40);
        detailsTextArea.setEditable(false);
        detailsTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane detailsScroll = new JScrollPane(detailsTextArea);
        bottomPanel.add(detailsScroll, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        startButton = new JButton("Start");
        cancelButton = new JButton("Cancel");
        actionPanel.add(startButton);
        actionPanel.add(cancelButton);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Listeners.
        searchButton.addActionListener(e -> populateAvailableList());
        addButton.addActionListener(e -> {
            for (SiteMapEntry entry : availableList.getSelectedValuesList()) {
                if (!chosenListModel.contains(entry)) {
                    chosenListModel.addElement(entry);
                }
            }
        });
        removeButton.addActionListener(e -> {
            for (SiteMapEntry entry : chosenList.getSelectedValuesList()) {
                chosenListModel.removeElement(entry);
            }
        });
        // Show details when an available entry is selected.
        availableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SiteMapEntry selected = availableList.getSelectedValue();
                if (selected != null) {
                    showEntryDetails(selected);
                }
            }
        });
    }

    /**
     * Populates the available list using the host and path prefix filters.
     */
    private void populateAvailableList() {
        availableListModel.clear();
        String host = hostField.getText().trim();
        String pathPrefix = pathField.getText().trim();

        if (host.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a host.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<HttpRequestResponse> entries = new ArrayList<>();

        // If the host doesn't start with a scheme, try both http and https.
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            String prefixHttp = "http://" + host;
            String prefixHttps = "https://" + host;
            if (!pathPrefix.isEmpty()) {
                prefixHttp += pathPrefix;
                prefixHttps += pathPrefix;
            }
            List<HttpRequestResponse> httpEntries = siteMap.requestResponses(SiteMapFilter.prefixFilter(prefixHttp));
            List<HttpRequestResponse> httpsEntries = siteMap.requestResponses(SiteMapFilter.prefixFilter(prefixHttps));
            if (httpEntries != null) {
                entries.addAll(httpEntries);
            }
            if (httpsEntries != null) {
                entries.addAll(httpsEntries);
            }
        } else {
            String prefix = host;
            if (!pathPrefix.isEmpty()) {
                prefix += pathPrefix;
            }
            List<HttpRequestResponse> list = siteMap.requestResponses(SiteMapFilter.prefixFilter(prefix));
            if (list != null) {
                entries.addAll(list);
            }
        }

        // Add entries to the available list.
        for (HttpRequestResponse entry : entries) {
            SiteMapEntry siteEntry = new SiteMapEntry(entry);
            availableListModel.addElement(siteEntry);
        }
    }

    /**
     * Displays the full request/response details of the given entry in the details area.
     */
    private void showEntryDetails(SiteMapEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REQUEST ===\n")
                .append(entry.getHttpRequest().toString())
                .append("\n\n=== RESPONSE ===\n");
        if (entry.getHttpResponse() != null) {
            sb.append(entry.getHttpResponse().toString());
        } else {
            sb.append("No response available.");
        }
        detailsTextArea.setText(sb.toString());
        detailsTextArea.setCaretPosition(0);
    }

    /**
     * Returns the chosen entries as a List.
     */
    public List<SiteMapEntry> getChosenEntries() {
        List<SiteMapEntry> list = new ArrayList<>();
        for (int i = 0; i < chosenListModel.size(); i++) {
            list.add(chosenListModel.get(i));
        }
        return list;
    }

    /**
     * Helper to extract HttpRequestResponse objects from a list of SiteMapEntry objects.
     */
    private List<HttpRequestResponse> extractHttpRequestResponses(List<SiteMapEntry> entries) {
        List<HttpRequestResponse> list = new ArrayList<>();
        for (SiteMapEntry entry : entries) {
            list.add(entry.getRequestResponse());
        }
        return list;
    }

    public boolean isStarted() {
        return started;
    }

    public String getHostname() {
        return hostField.getText().trim();
    }
}


/**
 * Handler interface for processing chosen entries.
 */
interface ProxyScopeHandler {
    void handleSelectedEntries(List<HttpRequestResponse> entries);
}

class SiteMapEntry {
    private final HttpRequestResponse requestResponse;

    public SiteMapEntry(HttpRequestResponse requestResponse) {
        this.requestResponse = requestResponse;
    }

    public HttpRequestResponse getRequestResponse() {
        return requestResponse;
    }

    public burp.api.montoya.http.message.requests.HttpRequest getHttpRequest() {
        return requestResponse.request();
    }

    public burp.api.montoya.http.message.responses.HttpResponse getHttpResponse() {
        return requestResponse.response();
    }

    @Override
    public String toString() {
        // Display the request method and path.
        String method = getHttpRequest().method();
        String path = getHttpRequest().path();
        return method + " " + path;
    }
}
