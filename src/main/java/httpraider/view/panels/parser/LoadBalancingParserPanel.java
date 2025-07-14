package httpraider.view.panels.parser;

import httpraider.model.network.LoadBalancingRule;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class LoadBalancingParserPanel extends JPanel {
    private final JPanel rulesPanel;
    private final JScrollPane scrollPane;
    private final JButton addButton;
    private final JPanel btnPanel;
    private final List<LoadBalancingRuleRowPanel> ruleRows = new ArrayList<>();
    private Map<String, String> eligibleProxies = new LinkedHashMap<>();
    private static final int GAP_TOP = 15;
    private static final int GAP_BETWEEN_ROWS = 40;
    private static final int GAP_BELOW_SCROLL = 20;

    public LoadBalancingParserPanel() {
        super();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        rulesPanel = new JPanel();
        rulesPanel.setLayout(new BoxLayout(rulesPanel, BoxLayout.Y_AXIS));
        rulesPanel.setOpaque(false);

        scrollPane = new JScrollPane(rulesPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);

        addButton = new JButton("+ Add Rule");
        addButton.addActionListener(e -> addRule(new LoadBalancingRule()));

        btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(addButton);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(GAP_BELOW_SCROLL, 0, 0, 0)); // gap above add button

        add(scrollPane, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    public void setEligibleProxies(Map<String, String> proxies) {
        this.eligibleProxies = new LinkedHashMap<>(proxies);
        for (LoadBalancingRuleRowPanel row : ruleRows) {
            row.setEligibleProxies(proxies);
        }
    }

    public void setRules(List<LoadBalancingRule> rules) {
        rulesPanel.removeAll();
        ruleRows.clear();
        // Always add a gap at the very top before the first rule
        rulesPanel.add(Box.createVerticalStrut(GAP_TOP));
        if (rules != null) {
            boolean first = true;
            for (LoadBalancingRule rule : rules) {
                if (!first) rulesPanel.add(Box.createVerticalStrut(GAP_BETWEEN_ROWS));
                addRuleInternal(rule);
                first = false;
            }
        }
        rulesPanel.revalidate();
        rulesPanel.repaint();
    }

    public List<LoadBalancingRule> getRules() {
        List<LoadBalancingRule> out = new ArrayList<>();
        for (LoadBalancingRuleRowPanel row : ruleRows) {
            out.add(row.toModel());
        }
        return out;
    }

    private void addRule(LoadBalancingRule rule) {
        // If no rules yet, ensure the top gap exists
        if (ruleRows.isEmpty()) {
            rulesPanel.removeAll();
            rulesPanel.add(Box.createVerticalStrut(GAP_TOP));
        } else {
            rulesPanel.add(Box.createVerticalStrut(GAP_BETWEEN_ROWS));
        }
        addRuleInternal(rule);
        rulesPanel.revalidate();
        rulesPanel.repaint();
    }

    private void addRuleInternal(LoadBalancingRule rule) {
        LoadBalancingRuleRowPanel row = new LoadBalancingRuleRowPanel(rule, eligibleProxies, this::removeRule);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        ruleRows.add(row);
        rulesPanel.add(row);
    }

    private void removeRule(LoadBalancingRuleRowPanel row) {
        int idx = ruleRows.indexOf(row);
        if (idx > 0) {
            // Remove gap above the row if not first
            rulesPanel.remove(idx * 2); // adjust for the extra top gap
        }
        ruleRows.remove(row);
        rulesPanel.remove(row);
        // If all rules removed, remove everything so addRule will re-add the top gap
        if (ruleRows.isEmpty()) {
            rulesPanel.removeAll();
        }
        rulesPanel.revalidate();
        rulesPanel.repaint();
    }
}
