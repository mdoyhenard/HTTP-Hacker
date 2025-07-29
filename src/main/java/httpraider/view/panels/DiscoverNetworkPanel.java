package httpraider.view.panels;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import proxyFinder.engine.*;
import proxyFinder.engine.MutationSet;
import proxyFinder.mutations.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Arrays;

public class DiscoverNetworkPanel extends JPanel {
    private final MontoyaApi montoyaApi;

    private final DefaultListModel<String> domainModel = new DefaultListModel<String>();
    private final JList<String> domainList = new JList<String>(domainModel);

    private final DefaultListModel<String> requestModel = new DefaultListModel<String>();
    private final JList<String> requestList = new JList<String>(requestModel);

    private final Map<String, List<HttpRequestResponse>> perDomain = new HashMap<String, List<HttpRequestResponse>>();
    private final Map<String, HttpRequestResponse> labelToHrr = new HashMap<String, HttpRequestResponse>();

    private final JButton detectButton = new JButton("Detect Proxy Chain");

    public DiscoverNetworkPanel(MontoyaApi api) {
        this.montoyaApi = api;

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

            Frame frame = JOptionPane.getFrameForComponent(this);
            JDialog progressDialog = new JDialog(frame, "Detecting Proxy Chain...", true);

            JLabel progressLabel = new JLabel("Detecting proxy chain... Please wait.");
            progressDialog.add(progressLabel, BorderLayout.CENTER);
            progressDialog.setSize(360, 100);
            progressDialog.setLocationRelativeTo(this);

            SwingWorker<List<ProxyBoundary>, Void> worker = new SwingWorker<List<ProxyBoundary>, Void>() {
                @Override
                protected List<ProxyBoundary> doInBackground() {
                    List<RequestMutationStrategy> mutations = Arrays.asList(
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
                    );

                    MutationRunner runner = new MutationRunner(montoyaApi, 3, 3, true);
                    List<MutationRunResult> runResults = runner.runAll(base, mutations);

                    Clusterer clusterer = new Clusterer();
                    List<ResponseCluster> clusters = clusterer.clusterResponses(runResults);

                    BoundaryInferer inferer = new BoundaryInferer();
                    return inferer.inferBoundaries(clusters);
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                    try {
                        List<ProxyBoundary> boundaries = get();
                        StringBuilder sb = new StringBuilder();
                        sb.append("Detected proxy boundaries:\n\n");
                        int idx = 1;
                        for (ProxyBoundary boundary : boundaries) {
                            sb.append(idx++).append(". Layer ").append(boundary.layer)
                                    .append(" (Median timing: ").append(boundary.medianTiming).append(" ms)\n");
                            for (ResponseCluster cluster : boundary.clusters) {
                                sb.append("    Error: [status=").append(cluster.statusCode).append("]\n");
                                sb.append("      Body: ").append(trimBody(cluster.normBody)).append("\n");
                                sb.append("      Headers: ").append(cluster.headers).append("\n");
                                sb.append("      Mutations: ");
                                for (MutationSet ms : cluster.getTriggeringMutations()) {
                                    sb.append(ms.describe()).append("; ");
                                }
                                sb.append("\n");
                            }
                            sb.append("\n");
                        }
                        JOptionPane.showMessageDialog(DiscoverNetworkPanel.this,
                                sb.toString(), "Proxy Chain", JOptionPane.INFORMATION_MESSAGE);

                        for (ProxyBoundary boundary : boundaries) {
                            System.out.println("=== Proxy Boundary: Layer " + boundary.layer + " (Median timing: " + boundary.medianTiming + ")");
                            for (ResponseCluster cluster : boundary.clusters) {
                                System.out.println("  Error: status=" + cluster.statusCode + ", body=" + trimBody(cluster.normBody));
                                System.out.println("  Headers: " + cluster.headers);
                                for (ClusterProof proof : cluster.proofs) {
                                    System.out.println("    Mutation: " + proof.mutationSet.describe());
                                    System.out.println("    Request: " + proof.response.request());
                                    if (proof.response.response() != null) {
                                        System.out.println("    Response status: " + proof.response.response().statusCode());
                                        System.out.println("    Response reason: " + proof.response.response().reasonPhrase());
                                    }
                                }
                            }
                            System.out.println("--------");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(DiscoverNetworkPanel.this,
                                "Error during proxy chain detection: " + ex.getMessage(),
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
