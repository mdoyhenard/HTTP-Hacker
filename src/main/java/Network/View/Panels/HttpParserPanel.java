package Network.View.Panels;

import RawRepeater.MsgLenHeader;
import Utils.HttpParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class HttpParserPanel extends JPanel {
    private JTextField headersEnd;
    private List<MessageLenRow> rows;
    private JPanel msgLenPanel;
    private final JPanel addButtonPanel;
    private TransformationPanel scriptPanel;

    public HttpParserPanel(HttpParser parser) {
        this.rows = new ArrayList<>();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header panel: label y campo de texto para "Headers End"
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel headerLabel = new JLabel("Header-break Pattern");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 15));;
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headersEnd = new JTextField(parser.getHeadersEnd(), 20);
        headersEnd.setAlignmentX(Component.CENTER_ALIGNMENT);
        headersEnd.setMaximumSize(headersEnd.getPreferredSize());
        headerPanel.add(headerLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        headerPanel.add(headersEnd);
        add(headerPanel);

        add(Box.createRigidArea(new Dimension(0, 10)));
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        //add(separator);
        add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel lenHeadersLabel = new JLabel("Message-length Headers");
        lenHeadersLabel.setFont(new Font("Arial", Font.BOLD, 15));;
        lenHeadersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(lenHeadersLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));

        // Panel de mesasge length headers (con filas para cada "message length")
        msgLenPanel = new JPanel();
        msgLenPanel.setLayout(new BoxLayout(msgLenPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(msgLenPanel);
        scrollPane.setPreferredSize(new Dimension(675, 125));
        scrollPane.setMaximumSize(scrollPane.getPreferredSize());
        add(scrollPane, BorderLayout.CENTER);

        JButton addButton = new JButton("+");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addMessageRow(new MsgLenHeader());
                msgLenPanel.revalidate();
                msgLenPanel.repaint();
            }
        });

        addButtonPanel = new JPanel();
        addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.X_AXIS));
        addButtonPanel.add(Box.createHorizontalGlue());
        addButtonPanel.add(addButton);
        addButtonPanel.add(Box.createRigidArea(new Dimension(5, 30)));
        msgLenPanel.add(addButtonPanel);


        add(Box.createRigidArea(new Dimension(0, 10)));

        JButton parseCodeButton = new JButton("Rewriting Script");
        add(parseCodeButton, BorderLayout.CENTER);

        parseCodeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scriptPanel = new TransformationPanel(parser.getParserCode());
                scriptPanel.setVisible(true);
            }
        });

        for (MsgLenHeader lenHeader : parser.getMessageLengths()){
            addMessageRow(lenHeader);
        }
    }

    private void closeScriptPanel(){
        scriptPanel.setVisible(false);
        scriptPanel = null;
    }

    public void saveParser(HttpParser baseParser){
        HttpParser parser = new HttpParser();
        parser.setHeadersEnd(getHeadersEnd());
        for (MessageLenRow row : rows){
            parser.addMessageLengths(row.getMsgLenHeader());
        }
        baseParser.clone(parser);
    }


    private void addMessageRow(MsgLenHeader lenHeader) {
        MessageLenRow row = new MessageLenRow(lenHeader);
        rows.add(row);
        msgLenPanel.remove(addButtonPanel);
        msgLenPanel.add(row);
        msgLenPanel.add(addButtonPanel);
    }

    public String getHeadersEnd() {
        return headersEnd.getText();
    }

    public void setHeadersEnd(JTextField headersEnd) {
        this.headersEnd = headersEnd;
    }

    public List<MessageLenRow> getRows() {
        return rows;
    }

    public void setRows(List<MessageLenRow> rows) {
        this.rows = rows;
    }

    private class MessageLenRow extends JPanel{
        private JCheckBox teCheck;
        private JCheckBox lastCheck;
        private JTextField mlField;

        private MessageLenRow(MsgLenHeader lenHeader){
            super();
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            this.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            teCheck = new JCheckBox("TE");
            this.add(teCheck);
            teCheck.setSelected(lenHeader.isTE());

            this.add(Box.createHorizontalStrut(5));

            lastCheck = new JCheckBox("Use last");
            this.add(lastCheck);
            lastCheck.setSelected(lenHeader.isUseLast());

            this.add(Box.createHorizontalStrut(5));

            mlField = new JTextField(lenHeader.getPattern(), 15);
            mlField.setPreferredSize(new Dimension(150, 25));
            this.add(mlField);
            this.add(Box.createHorizontalStrut(10));

            JButton removeButton = new JButton("-");
            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    rows.remove(MessageLenRow.this);
                    msgLenPanel.remove(MessageLenRow.this);
                    msgLenPanel.revalidate();
                    msgLenPanel.repaint();
                }
            });
            this.add(removeButton);
        }

        private boolean getTE(){
            return this.teCheck.isSelected();
        }

        private boolean getLast(){
            return this.lastCheck.isSelected();
        }

        private String getPattern(){
            return this.mlField.getText();
        }

        private MsgLenHeader getMsgLenHeader(){
            return new MsgLenHeader(getTE(), getLast(), getPattern());
        }
    }
}
