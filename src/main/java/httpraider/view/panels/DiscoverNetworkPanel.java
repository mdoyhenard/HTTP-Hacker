package httpraider.view.panels;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import httpraider.controller.NetworkController;
import httpraider.model.network.ProxyModel;
import httpraider.model.network.HttpParserModel;
import proxyFinder.engine.*;
import proxyFinder.engine.MutationSet;
import proxyFinder.mutations.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Arrays;
import java.text.SimpleDateFormat;

public class DiscoverNetworkPanel extends JPanel {
    private final MontoyaApi montoyaApi;
    private final NetworkController networkController;

    private final DefaultListModel<String> domainModel = new DefaultListModel<String>();
    private final JList<String> domainList = new JList<String>(domainModel);

    private final DefaultListModel<String> requestModel = new DefaultListModel<String>();
    private final JList<String> requestList = new JList<String>(requestModel);

    private final Map<String, List<HttpRequestResponse>> perDomain = new HashMap<String, List<HttpRequestResponse>>();
    private final Map<String, HttpRequestResponse> labelToHrr = new HashMap<String, HttpRequestResponse>();

    private final JButton detectButton = new JButton("Detect Proxy Chain");

    public DiscoverNetworkPanel(MontoyaApi api, NetworkController networkController) {
        this.montoyaApi = api;
        this.networkController = networkController;

        setLayout(new BorderLayout(10, 10));
        add(createScrollPanel(new JLabel("Select a domain:"), domainList), BorderLayout.WEST);
        add(createScrollPanel(new JLabel("Select a request/response:"), requestList), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        detectButton.setEnabled(false);
        btnPanel.add(detectButton);
        add(btnPanel, BorderLayout.SOUTH);

        domainList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        domainList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadRequestsForDomain(domainList.getSelectedValue());
            }
        });

        requestList.addListSelectionListener(e -> {
            detectButton.setEnabled(requestList.getSelectedIndex() != -1);
        });

        detectButton.addActionListener(e -> {
            HttpRequestResponse hrr = getSelectedRequestResponse();
            if (hrr == null) return;
            HttpRequest base = hrr.request();
            
            // Ask user for detection mode
            String[] options = {"Fast (Recommended)", "Thorough"};
            int choice = JOptionPane.showOptionDialog(this,
                "Select detection mode:\n\n" +
                "Fast: Quick scan with 4 essential mutations (~10-30 seconds)\n" +
                "   • Single samples, no combinations\n\n" +
                "Thorough: Complete scan with 20 mutations (2-5 minutes)\n" +
                "   • Multiple samples and combinations\n" +
                "   • More accurate parser configuration detection",
                "Detection Mode",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            boolean fastMode = (choice == 0 || choice == -1); // Default to fast mode

            Frame frame = JOptionPane.getFrameForComponent(this);
            JDialog progressDialog = new JDialog(frame, "Detecting Proxy Chain...", false); // non-modal

            JLabel progressLabel = new JLabel("Initializing detection...", SwingConstants.CENTER);
            progressLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            progressDialog.add(progressLabel, BorderLayout.CENTER);
            progressDialog.setSize(400, 120);
            progressDialog.setLocationRelativeTo(this);
            progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            
            final boolean isFastMode = fastMode;

            SwingWorker<List<ProxyBoundary>, String> worker = new SwingWorker<List<ProxyBoundary>, String>() {
                @Override
                protected List<ProxyBoundary> doInBackground() {
                    publish("Initializing mutations...");
                    
                    List<RequestMutationStrategy> mutations;
                    
                    if (isFastMode) {
                        // Fast mode: Only the most essential mutations
                        mutations = new ArrayList<>(Arrays.asList(
                                new InvalidVersionMutation(),
                                new CLandTEMutation(),
                                new InvalidHeaderNameMutation(),
                                new MalformedHeaderStructureMutation()
                        ));
                    } else {
                        // Thorough mode: Full comprehensive set
                        mutations = new ArrayList<>(Arrays.asList(
                                new InvalidCharInMethodMutation(),
                                new NullByteInPathMutation(),
                                new InvalidVersionMutation(),
                                new DuplicateContentLengthHeaderMutation(),
                                new InvalidHeaderNameMutation(),
                                new InvalidHeaderValueMutation(),
                                new MalformedHeaderStructureMutation(),
                                new InvalidBodyMutation(),
                                new CLandTEMutation(),
                                new NonPrintableInHeaderValueMutation(),
                                new SpaceInHeaderNameMutation(),
                                new OverlongMethodMutation(),
                                new ScriptTagInPathMutation(),
                                new TraceMethodMutation(),
                                new NullByteInHeaderNameMutation(),
                                new EncodedHeaderNameMutation(),
                                new ScriptTagInHeaderNameMutation(),
                                new FoldedContentLengthHeaderMutation(),
                                new ChunkedBodyMalformedMutation(),
                                new UpgradeHeaderMutation()
                        ));
                    }

                    publish("Running baseline request first...");
                    
                    // First, send a baseline request without mutations
                    HttpRequestResponse baselineResp = null;
                    long baselineTime = 0;
                    try {
                        long start = System.nanoTime();
                        baselineResp = montoyaApi.http().sendRequest(base);
                        baselineTime = (System.nanoTime() - start) / 1_000_000;
                        if (baselineResp != null && baselineResp.response() != null) {
                            publish("Baseline response: " + baselineResp.response().statusCode() + 
                                   " in " + baselineTime + "ms");
                        } else {
                            publish("No baseline response received");
                        }
                    } catch (Exception e) {
                        publish("Baseline request failed: " + e.getMessage());
                    }
                    
                    publish("Running " + mutations.size() + " mutations...");
                    
                    // Use optimized runner with timeout and progress updates
                    int sampleCount = isFastMode ? 1 : 2;
                    int combinationSize = isFastMode ? 1 : 2;
                    int timeout = isFastMode ? 5 : 10;
                    
                    OptimizedMutationRunner runner = new OptimizedMutationRunner(
                        montoyaApi, 
                        sampleCount,     // 1 for fast, 2 for thorough
                        combinationSize, // 1 for fast, 2 for thorough
                        false,          // enableSuppression=false
                        timeout,        // 5 seconds for fast, 10 for thorough
                        this::publish   // progress callback
                    );
                    
                    List<MutationRunResult> runResults;
                    try {
                        runResults = runner.runAll(base, mutations);
                    } finally {
                        runner.shutdown();
                    }

                    if (runResults == null || runResults.isEmpty()) {
                        publish("No responses received from mutations");
                        return new ArrayList<>();
                    }
                    
                    publish("Analyzing " + runResults.size() + " responses...");
                    
                    Clusterer clusterer = new Clusterer();
                    List<ResponseCluster> clusters = clusterer.clusterResponses(runResults);
                    
                    if (clusters == null || clusters.isEmpty()) {
                        publish("No response clusters found");
                        return new ArrayList<>();
                    }

                    publish("Found " + clusters.size() + " unique response clusters:");
                    
                    // Debug: Show what clusters were found
                    for (ResponseCluster cluster : clusters) {
                        publish("  - Status " + cluster.statusCode + ", " + 
                               cluster.proofs.size() + " occurrences, " +
                               "median time: " + cluster.medianTiming() + "ms");
                    }
                    
                    // If we only have one cluster, it means all mutations got the same response
                    // This suggests we're hitting the backend directly
                    if (clusters.size() == 1) {
                        publish("All mutations returned identical responses - likely direct backend connection");
                    }

                    publish("Inferring proxy boundaries from " + clusters.size() + " clusters...");
                    
                    BoundaryInferer inferer = new BoundaryInferer();
                    List<ProxyBoundary> boundaries = inferer.inferBoundaries(clusters);
                    
                    return boundaries != null ? boundaries : new ArrayList<>();
                }
                
                @Override
                protected void process(List<String> chunks) {
                    for (String message : chunks) {
                        progressLabel.setText(message);
                    }
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                    try {
                        List<ProxyBoundary> boundaries = get();
                        
                        // Always add backend server as the final proxy even if no boundaries detected
                        if (boundaries.isEmpty()) {
                            publish("No intermediate proxies detected, adding backend server...");
                            // Create a synthetic boundary for the backend server
                            List<ResponseCluster> backendClusters = new ArrayList<>();
                            ProxyBoundary backendBoundary = new ProxyBoundary(1, backendClusters);
                            boundaries = new ArrayList<>();
                            boundaries.add(backendBoundary);
                        }
                        
                        // Get timestamp and base request info
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String timestamp = sdf.format(new Date());
                        String basePath = base.path();
                        String baseHost = base.httpService().host();
                        
                        // Clear existing proxies (except client) and connections
                        List<String> toRemove = new ArrayList<>();
                        for (ProxyModel proxy : networkController.getModel().getProxies()) {
                            if (!proxy.isClient()) {
                                toRemove.add(proxy.getId());
                            }
                        }
                        toRemove.forEach(id -> networkController.getModel().removeProxy(id));
                        networkController.getModel().getConnections().clear();
                        
                        // Create proxies for each discovered boundary
                        List<ProxyModel> createdProxies = new ArrayList<>();
                        for (int i = 0; i < boundaries.size(); i++) {
                            ProxyBoundary boundary = boundaries.get(i);
                            
                            // Create proxy name and description
                            String proxyName;
                            boolean isBackend = (i == boundaries.size() - 1 && boundary.clusters.isEmpty());
                            
                            if (isBackend) {
                                proxyName = "Backend Server (" + baseHost + ")";
                            } else {
                                proxyName = "Proxy " + (i + 1);
                            }
                            
                            StringBuilder description = new StringBuilder();
                            description.append("[Network Discovery result : ").append(basePath).append(" : ").append(timestamp).append("]\n\n");
                            
                            if (!isBackend) {
                                description.append("Layer: ").append(boundary.layer).append("\n");
                                description.append("Median timing: ").append(boundary.medianTiming).append(" ms\n\n");
                                description.append("Detected behaviors:\n");
                            } else {
                                description.append("Role: Backend Server\n");
                                description.append("Host: ").append(baseHost).append("\n");
                                description.append("No proxy-specific behaviors detected\n");
                            }
                            
                            // Analyze mutations and set parser config based on detected behaviors
                            ProxyModel proxy = new ProxyModel(proxyName);
                            proxy.setShowParser(true);
                            
                            // Collect all triggering mutations for this boundary
                            Set<String> allMutations = new HashSet<>();
                            if (!isBackend) {
                                for (ResponseCluster cluster : boundary.clusters) {
                                    description.append("- Status ").append(cluster.statusCode).append(": ");
                                    description.append(trimBody(cluster.normBody)).append("\n");
                                    
                                    for (MutationSet ms : cluster.getTriggeringMutations()) {
                                        if (ms != null && ms.getMutations() != null) {
                                            for (RequestMutationStrategy mutation : ms.getMutations()) {
                                                if (mutation != null) {
                                                    allMutations.add(mutation.getClass().getSimpleName());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Configure parser based on detected mutations
                            HttpParserModel parserSettings = proxy.getParserSettings();
                            
                            if (allMutations.contains("CLandTEMutation")) {
                                // Configure to prioritize Content-Length over Transfer-Encoding
                                List<HttpParserModel.BodyLenHeaderRule> rules = new ArrayList<>();
                                rules.add(new HttpParserModel.BodyLenHeaderRule("Content-Length: ", false, HttpParserModel.DuplicateHandling.FIRST));
                                rules.add(new HttpParserModel.BodyLenHeaderRule("Transfer-Encoding: chunked", true, HttpParserModel.DuplicateHandling.LAST));
                                parserSettings.setBodyLenHeaderRules(rules);
                                description.append("\nParser config: Content-Length priority\n");
                            }
                            
                            if (allMutations.contains("ChunkedBodyMalformedMutation")) {
                                // Set output to force Content-Length to handle malformed chunked encoding
                                parserSettings.setOutputBodyEncoding(HttpParserModel.MessageLenBodyEncoding.FORCE_CL_HEADER);
                                description.append("Parser config: Force Content-Length output\n");
                            }
                            
                            if (allMutations.contains("InvalidHeaderValueMutation") || 
                                allMutations.contains("NonPrintableInHeaderValueMutation") ||
                                allMutations.contains("SpaceInHeaderNameMutation")) {
                                // Add custom header line endings to handle malformed headers
                                List<String> headerEndings = new ArrayList<>(parserSettings.getHeaderLineEndings());
                                if (!headerEndings.contains("\\n")) {
                                    headerEndings.add("\\n");
                                }
                                parserSettings.setHeaderLineEndings(headerEndings);
                                description.append("Parser config: Relaxed header line endings\n");
                            }
                            
                            if (allMutations.contains("FoldedContentLengthHeaderMutation") ||
                                allMutations.contains("MalformedHeaderStructureMutation")) {
                                parserSettings.setAllowHeaderFolding(true);
                                description.append("Parser config: Header line folding enabled\n");
                            }
                            
                            if (allMutations.contains("InvalidVersionMutation")) {
                                parserSettings.setForcedHttpVersion(HttpParserModel.ForcedHttpVersion.HTTP_1_1);
                                description.append("Parser config: Force HTTP/1.1\n");
                            }
                            
                            if (!allMutations.isEmpty()) {
                                description.append("\nTriggering mutations: ");
                                description.append(String.join(", ", allMutations));
                            }
                            
                            proxy.setDescription(description.toString());
                            
                            // Add proxy to network
                            networkController.addProxyQuick(proxy);
                            createdProxies.add(proxy);
                        }
                        
                        // Connect proxies in chain (Client -> Proxy1 -> Proxy2 -> ...)
                        String clientId = ProxyModel.CLIENT_ID;
                        if (!createdProxies.isEmpty()) {
                            // Connect client to first proxy
                            networkController.addConnectionQuick(clientId, createdProxies.get(0).getId());
                            
                            // Connect each proxy to the next
                            for (int i = 0; i < createdProxies.size() - 1; i++) {
                                networkController.addConnectionQuick(
                                    createdProxies.get(i).getId(),
                                    createdProxies.get(i + 1).getId()
                                );
                            }
                        }
                        
                        // Force refresh the view
                        networkController.refreshView();
                        
                        // Show summary
                        JOptionPane.showMessageDialog(DiscoverNetworkPanel.this,
                                "Network discovery completed!\n\n" +
                                "Discovered " + boundaries.size() + " proxy boundaries.\n" +
                                "Created proxy chain: Client -> " + 
                                String.join(" -> ", createdProxies.stream()
                                    .map(ProxyModel::getDomainName)
                                    .toArray(String[]::new)),
                                "Network Discovery Complete", JOptionPane.INFORMATION_MESSAGE);
                        
                    } catch (Exception ex) {
                        String errorMsg = "Error during proxy chain detection:\n" + 
                                         ex.getClass().getSimpleName() + ": " + ex.getMessage();
                        if (ex.getCause() != null) {
                            errorMsg += "\nCause: " + ex.getCause().getMessage();
                        }
                        JOptionPane.showMessageDialog(DiscoverNetworkPanel.this,
                                errorMsg,
                                "Error", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            };

            worker.execute();
            progressDialog.setVisible(true);
        });

        loadDomains();
    }

    private JPanel createScrollPanel(JLabel label, JList<String> list) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    private void loadDomains() {
        domainModel.clear();
        perDomain.clear();

        List<HttpRequestResponse> all = montoyaApi.siteMap().requestResponses();
        for (HttpRequestResponse hrr : all) {
            String host = hrr.httpService().host();
            perDomain.computeIfAbsent(host, k -> new ArrayList<HttpRequestResponse>()).add(hrr);
        }

        List<String> sorted = new ArrayList<String>(perDomain.keySet());
        Collections.sort(sorted);
        for (String domain : sorted) {
            domainModel.addElement(domain);
        }
    }

    private void loadRequestsForDomain(String domain) {
        requestModel.clear();
        labelToHrr.clear();
        detectButton.setEnabled(false);

        if (domain == null) return;
        List<HttpRequestResponse> reqs = perDomain.getOrDefault(domain, Collections.emptyList());
        int idx = 0;
        for (HttpRequestResponse hrr : reqs) {
            String method = hrr.request().method();
            String path = hrr.request().path();
            String label = method + " " + path + " [" + idx + "]";
            requestModel.addElement(label);
            labelToHrr.put(label, hrr);
            idx++;
        }
    }

    private HttpRequestResponse getSelectedRequestResponse() {
        return labelToHrr.get(requestList.getSelectedValue());
    }

    private static String trimBody(String body) {
        if (body == null) return "";
        String s = body.trim();
        return s.length() <= 100 ? s : s.substring(0, 100) + "...";
    }
}
