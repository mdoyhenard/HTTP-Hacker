package httpraider.view.panels.parser;

import httpraider.model.network.FirewallRule;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class FirewallRulesParserPanel extends JPanel {
    private final JPanel rulesPanel;
    private final JScrollPane scrollPane;
    private final JButton addButton;
    private final JPanel btnPanel;
    private final List<FirewallRuleRowPanel> ruleRows = new ArrayList<>();
    private static final int GAP_TOP = 15;
    private static final int GAP_BETWEEN_ROWS = 40;
    private static final int GAP_BELOW_SCROLL = 20;

    public FirewallRulesParserPanel() {
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
        addButton.addActionListener(e -> addRule(new FirewallRule()));

        btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(addButton);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(GAP_BELOW_SCROLL, 0, 0, 0));

        add(scrollPane, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    public void setRules(List<FirewallRule> rules) {
        rulesPanel.removeAll();
        ruleRows.clear();
        rulesPanel.add(Box.createVerticalStrut(GAP_TOP));
        if (rules != null) {
            boolean first = true;
            for (FirewallRule rule : rules) {
                if (!first) rulesPanel.add(Box.createVerticalStrut(GAP_BETWEEN_ROWS));
                addRuleInternal(rule);
                first = false;
            }
        }
        rulesPanel.revalidate();
        rulesPanel.repaint();
    }

    public List<FirewallRule> getRules() {
        List<FirewallRule> out = new ArrayList<>();
        for (FirewallRuleRowPanel row : ruleRows) {
            out.add(row.toModel());
        }
        return out;
    }

    private void addRule(FirewallRule rule) {
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

    private void addRuleInternal(FirewallRule rule) {
        FirewallRuleRowPanel row = new FirewallRuleRowPanel(rule, this::removeRule);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        ruleRows.add(row);
        rulesPanel.add(row);
    }

    private void removeRule(FirewallRuleRowPanel row) {
        int idx = ruleRows.indexOf(row);
        if (idx > 0) {
            rulesPanel.remove(idx * 2);
        }
        ruleRows.remove(row);
        rulesPanel.remove(row);
        if (ruleRows.isEmpty()) {
            rulesPanel.removeAll();
        }
        rulesPanel.revalidate();
        rulesPanel.repaint();
    }
}