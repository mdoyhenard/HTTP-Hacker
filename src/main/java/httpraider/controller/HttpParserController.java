package httpraider.controller;

import httpraider.model.network.BodyLenHeader;
import httpraider.model.network.HttpParserModel;
import httpraider.view.panels.*;

import java.util.List;

public class HttpParserController {
    private final HttpParserModel settings;
    private final HttpParserPanel parserPanel;

    public HttpParserController(HttpParserModel settings, HttpParserPanel parserPanel) {
        this.settings = settings;
        this.parserPanel = parserPanel;

        installListeners();
        loadFromModel();
    }

    private void installListeners() {
        ParserSettingsPanel settingsPanel = parserPanel.getSettingsPanel();

        settingsPanel.getHeadersEndPanel().setAddButtonListener(e -> {
            settingsPanel.getHeadersEndPanel().addRow();
            updateRemoveListeners(settingsPanel.getHeadersEndPanel());
        });

        settingsPanel.getHeaderSplittingPanel().setAddButtonListener(e -> {
            settingsPanel.getHeaderSplittingPanel().addRow();
            updateRemoveListeners(settingsPanel.getHeaderSplittingPanel());
        });

        settingsPanel.getMessageLengthPanel().setAddButtonListener(e -> {
            settingsPanel.getMessageLengthPanel().addRow();
            updateRemoveListeners(settingsPanel.getMessageLengthPanel());
        });

        settingsPanel.getChunkLineEndPanel().setAddButtonListener(e -> {
            settingsPanel.getChunkLineEndPanel().addRow();
            updateRemoveListeners(settingsPanel.getChunkLineEndPanel());
        });

        parserPanel.getSaveButton().addActionListener(e -> saveToModel());
    }

    private void updateRemoveListeners(ParserSettingsSubPanel panel) {
        List<ParserSettingsRowPanel> rows = panel.getRows();
        for (int i = 0; i < rows.size(); i++) {
            ParserSettingsRowPanel row = rows.get(i);
            panel.setRemoveButtonListener(row, e -> {
                panel.removeRow(row);
                updateRemoveListeners(panel);
            });
        }
    }

    private void loadFromModel() {
        ParserSettingsPanel settingsPanel = parserPanel.getSettingsPanel();
        ParserCodePanel codePanel = parserPanel.getCodePanel();

        settingsPanel.getHeadersEndPanel().clearRows();
        for (String seq : settings.getHeaderEndSequences()) {
            settingsPanel.getHeadersEndPanel().addRow();
            List<ParserSettingsRowPanel> rows = settingsPanel.getHeadersEndPanel().getRows();
            rows.get(rows.size() - 1).getTextField().setText(seq);
        }
        updateRemoveListeners(settingsPanel.getHeadersEndPanel());

        settingsPanel.getHeaderSplittingPanel().clearRows();
        for (String seq : settings.getHeaderSplitSequences()) {
            settingsPanel.getHeaderSplittingPanel().addRow();
            List<ParserSettingsRowPanel> rows = settingsPanel.getHeaderSplittingPanel().getRows();
            rows.get(rows.size() - 1).getTextField().setText(seq);
        }
        updateRemoveListeners(settingsPanel.getHeaderSplittingPanel());

        settingsPanel.getMessageLengthPanel().clearRows();
        for (BodyLenHeader h : settings.getBodyLenHeaders()) {
            settingsPanel.getMessageLengthPanel().addRow();
            List<ParserSettingsRowPanel> rows = settingsPanel.getMessageLengthPanel().getRows();
            ParserSettingsRowPanel row = rows.get(rows.size() - 1);
            row.getTextField().setText(h.getHeaderNamePattern());
            if (row.getCheckBox() != null) {
                row.getCheckBox().setSelected(h.isChunkedEncoding());
            }
        }

        settingsPanel.getChunkLineEndPanel().clearRows();
        for (String seq : settings.getChunkEndSequences()) {
            settingsPanel.getChunkLineEndPanel().addRow();
            List<ParserSettingsRowPanel> rows = settingsPanel.getChunkLineEndPanel().getRows();
            rows.get(rows.size() - 1).getTextField().setText(seq);
        }
        updateRemoveListeners(settingsPanel.getChunkLineEndPanel());

        codePanel.setCode1(settings.getCodeStep1());
        codePanel.setCode2(settings.getCodeStep2());
        codePanel.setCode3(settings.getCodeStep3());

        settingsPanel.updateChunkPanelVisibility(); // <-- THIS LINE FIXES YOUR ISSUE
    }

    private void saveToModel() {
        ParserSettingsPanel settingsPanel = parserPanel.getSettingsPanel();
        ParserCodePanel codePanel = parserPanel.getCodePanel();

        settings.setHeaderEndSequences(settingsPanel.getHeadersEndPanel().getAllRowTexts());
        settings.setHeaderSplitSequences(settingsPanel.getHeaderSplittingPanel().getAllRowTexts());
        settings.setBodyLenHeaders(settingsPanel.getMessageLengthPanel().getAllBodyLenHeaders());
        settings.setChunkEndSequences(settingsPanel.getChunkLineEndPanel().getAllRowTexts());

        settings.setCodeStep1(codePanel.getCode1());
        settings.setCodeStep2(codePanel.getCode2());
        settings.setCodeStep3(codePanel.getCode3());
    }

}
