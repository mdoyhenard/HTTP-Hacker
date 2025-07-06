package httpraider.controller;

import httpraider.controller.engines.JSEngine;
import httpraider.model.network.BodyLenHeader;
import httpraider.model.network.HttpParserModel;
import httpraider.model.network.ProxyModel;
import httpraider.view.panels.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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

        codePanel.setCode1(settings.getHeaderEndCode());
        codePanel.setCode2(settings.getHeaderSplitCode());
        codePanel.setCode3(settings.getBodyLenCode());

        settingsPanel.updateChunkPanelVisibility(); // <-- THIS LINE FIXES YOUR ISSUE
    }

    private void saveToModel() {
        ParserSettingsPanel settingsPanel = parserPanel.getSettingsPanel();
        ParserCodePanel codePanel = parserPanel.getCodePanel();

        settings.setHeaderEndSequences(settingsPanel.getHeadersEndPanel().getAllRowTexts());
        settings.setHeaderSplitSequences(settingsPanel.getHeaderSplittingPanel().getAllRowTexts());
        settings.setBodyLenHeaders(settingsPanel.getMessageLengthPanel().getAllBodyLenHeaders());
        settings.setChunkEndSequences(settingsPanel.getChunkLineEndPanel().getAllRowTexts());

        settings.setHeaderEndCode(codePanel.getCode1());
        settings.setHeaderSplitCode(codePanel.getCode2());
        settings.setBodyLenCode(codePanel.getCode3());
    }

    private static byte[] decodeEscapedSequence(String s) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                if (next == 'r') {
                    result.append('\r');
                    i += 2;
                } else if (next == 'n') {
                    result.append('\n');
                    i += 2;
                } else if (next == 't') {
                    result.append('\t');
                    i += 2;
                } else if (next == '\\') {
                    result.append('\\');
                    i += 2;
                } else {
                    result.append(next);
                    i += 2;
                }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    public static byte[][] splitHeaders(HttpParserModel model, byte[] data) {
        List<String> seqs = model.getHeaderEndSequences();
        int splitIndex = -1;
        byte[] foundSeq = null;
        for (String seq : seqs) {
            byte[] seqBytes = decodeEscapedSequence(seq);
            int idx = indexOf(data, seqBytes);
            if (idx >= 0 && (splitIndex == -1 || idx < splitIndex)) {
                splitIndex = idx;
                foundSeq = seqBytes;
            }
        }
        if (splitIndex == -1 || foundSeq == null) {
            if (true) return JSEngine.runEndHeaderScript(data, new byte[0], model.getHeaderEndCode());
            return new byte[][] { data, new byte[0] };
        }
        int end = splitIndex + foundSeq.length;
        byte[] headers = Arrays.copyOfRange(data, 0, end);
        byte[] rest = Arrays.copyOfRange(data, end, data.length);
        if (true) return JSEngine.runEndHeaderScript(headers, rest, model.getHeaderEndCode());
        return new byte[][] { headers, rest };
    }

    private static int indexOf(byte[] data, byte[] pattern) {
        outer: for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    public static List<String> splitHeaderLines(HttpParserModel model, byte[] headersBytes) {
        String headers = new String(headersBytes, StandardCharsets.ISO_8859_1);
        List<String> seqs = model.getHeaderSplitSequences();
        int splitIdx = -1;
        String foundSeq = null;
        for (String seq : seqs) {
            String actualSeq = decodeEscapedSequenceStr(seq);
            int idx = headers.indexOf(actualSeq);
            if (idx != -1 && (splitIdx == -1 || idx < splitIdx)) {
                splitIdx = idx;
                foundSeq = actualSeq;
            }
        }
        if (foundSeq == null) {
            List<String> l = new ArrayList<>();
            l.add(headers);
            if (true) return JSEngine.runSplitHeaderScript(l, headersBytes, model.getHeaderSplitCode());
            return l;
        }
        List<String> result = new ArrayList<>(Arrays.asList(headers.split(java.util.regex.Pattern.quote(foundSeq), -1)));
        if (result.size() > 0 && result.get(result.size() - 1).isEmpty()) {
            result.remove(result.size() - 1);
        }
        if (true) return JSEngine.runSplitHeaderScript(result, headersBytes, model.getHeaderSplitCode());
        return result;
    }

    private static String decodeEscapedSequenceStr(String s) {
        return new String(decodeEscapedSequence(s), StandardCharsets.ISO_8859_1);
    }

    private static List<byte[]> parseRequests(HttpParserModel model, byte[] data) {
        List<byte[]> requests = new ArrayList<>();
        byte[] remaining = data;
        while (remaining.length > 0) {
            int startLength = remaining.length;

            // Step 1: Split headers
            byte[][] headersSplit = splitHeaders(model, remaining);
            byte[] headersBytes = headersSplit[0];
            byte[] rest = headersSplit[1];

            // Step 2: Get header lines
            List<String> headerLines = splitHeaderLines(model, headersBytes);

            // Step 3: Get body and remaining data after body
            byte[][] bodySplit = getBody(model, headerLines, rest);
            byte[] bodyBytes = bodySplit[0];
            byte[] afterBody = bodySplit[1];

            // The full request: headers + body
            byte[] fullRequest = new byte[headersBytes.length + bodyBytes.length];
            System.arraycopy(headersBytes, 0, fullRequest, 0, headersBytes.length);
            System.arraycopy(bodyBytes, 0, fullRequest, headersBytes.length, bodyBytes.length);
            requests.add(fullRequest);

            // Move to the next part
            remaining = afterBody;

            // Break if no progress (avoid infinite loop)
            if (remaining.length == startLength) {
                break;
            }
            // If header is empty (should never happen with valid input), break
            if (headersBytes.length == 0) {
                break;
            }
        }
        return requests;
    }


    public static byte[][] getBody(HttpParserModel model, List<String> headerLines, byte[] body) {
        List<BodyLenHeader> headers = model.getBodyLenHeaders();
        for (BodyLenHeader h : headers) {
            String pattern = h.getHeaderNamePattern();
            for (String headerLine : headerLines) {
                if (headerLine.startsWith(pattern)) {
                    if (h.isChunkedEncoding()) {
                        return parseChunked(body, model.getChunkEndSequences());
                    } else {
                        String rest = headerLine.substring(pattern.length()).trim();
                        int len;
                        try {
                            int idx = rest.indexOf(' ');
                            if (idx > 0) rest = rest.substring(0, idx);
                            idx = rest.indexOf(';');
                            if (idx > 0) rest = rest.substring(0, idx);
                            len = Integer.parseInt(rest);
                        } catch (Exception e) {
                            if (true) return JSEngine.runBodyLenScript(headerLines, new byte[0], body, model.getBodyLenCode());
                            return new byte[][] { new byte[0], body };
                        }
                        if (len >= body.length) {
                            if (true) return JSEngine.runBodyLenScript(headerLines, body, new byte[0], model.getBodyLenCode());
                            return new byte[][] { body, new byte[0] };
                        }
                        byte[] b1 = Arrays.copyOfRange(body, 0, len);
                        byte[] b2 = Arrays.copyOfRange(body, len, body.length);
                        if (true) return JSEngine.runBodyLenScript(headerLines, b1, b2, model.getBodyLenCode());
                        return new byte[][] { b1, b2 };
                    }
                }
            }
        }
        if (true) return JSEngine.runBodyLenScript(headerLines, new byte[0], body, model.getBodyLenCode());
        return new byte[][] { new byte[0], body };
    }

    private static byte[][] parseChunked(byte[] body, List<String> chunkEndSeqs) {
        int idx = 0;
        List<byte[]> chunks = new ArrayList<>();
        while (idx < body.length) {
            int[] seqPos = findFirstSeq(body, idx, chunkEndSeqs);
            if (seqPos[0] == -1) {
                break;
            }
            int sizeLineEnd = seqPos[0];
            String sizeLine = new String(body, idx, sizeLineEnd - idx, StandardCharsets.ISO_8859_1).trim();
            int chunkSize;
            try {
                int semi = sizeLine.indexOf(';');
                if (semi != -1) sizeLine = sizeLine.substring(0, semi).trim();
                chunkSize = Integer.parseInt(sizeLine, 16);
            } catch (Exception e) {
                break;
            }
            int seqLen = seqPos[1];
            int chunkStart = sizeLineEnd + seqLen;
            if (chunkSize == 0) {
                int[] endPos = findFirstSeq(body, chunkStart, chunkEndSeqs);
                if (endPos[0] == -1) {
                    chunks.add(Arrays.copyOfRange(body, chunkStart, body.length));
                    idx = body.length;
                } else {
                    chunks.add(Arrays.copyOfRange(body, chunkStart, endPos[0]));
                    idx = endPos[0] + endPos[1];
                }
                break;
            }
            if (chunkStart + chunkSize > body.length) {
                chunks.add(Arrays.copyOfRange(body, chunkStart, body.length));
                idx = body.length;
                break;
            }
            chunks.add(Arrays.copyOfRange(body, chunkStart, chunkStart + chunkSize));
            int nextIdx = chunkStart + chunkSize;
            int[] nextSeq = findFirstSeq(body, nextIdx, chunkEndSeqs);
            if (nextSeq[0] == -1) {
                idx = nextIdx;
                break;
            }
            idx = nextSeq[0] + nextSeq[1];
        }
        int total = chunks.stream().mapToInt(b -> b.length).sum();
        byte[] joined = new byte[total];
        int pos = 0;
        for (byte[] c : chunks) {
            System.arraycopy(c, 0, joined, pos, c.length);
            pos += c.length;
        }
        if (idx >= body.length) {
            return new byte[][] { joined, new byte[0] };
        }
        return new byte[][] { joined, Arrays.copyOfRange(body, idx, body.length) };
    }

    private static int[] findFirstSeq(byte[] data, int start, List<String> seqs) {
        int firstIdx = -1, seqLen = 0;
        for (String seq : seqs) {
            byte[] s = decodeEscapedSequence(seq);
            int idx = indexOf(data, s, start);
            if (idx >= 0 && (firstIdx == -1 || idx < firstIdx)) {
                firstIdx = idx;
                seqLen = s.length;
            }
        }
        return new int[] { firstIdx, seqLen };
    }

    private static int indexOf(byte[] data, byte[] pattern, int start) {
        outer: for (int i = start; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    public static void setRequestGroupsEditor(ProxyModel proxyModel, List<byte[]> baseRequests, NetworkController networkController, HttpMultiEditorPanel proxyEditor){
        List<byte[]> requests = new ArrayList<>(baseRequests);
        List<ProxyModel> connectionList = networkController.getConnectionPathToClient(proxyModel.getId());
        if (connectionList != null) {
            for (ProxyModel proxyList : connectionList) {
                List<byte[]> parsedRequests = new ArrayList<>();
                for (byte[] request : requests) {
                    parsedRequests.addAll(HttpParserController.parseRequests(proxyList.getParserSettings(), request));
                }
                requests = parsedRequests;
            }
        }
        proxyEditor.clear();
        for (byte[] request : requests){
            proxyEditor.addGroup(HttpParserController.parseRequests(proxyModel.getParserSettings(), request));
        }
    }

    public static void setRequestGroupsEditor(ProxyModel proxyModel, byte[] baseRequest, NetworkController networkController, HttpMultiEditorPanel proxyEditor){
        List<byte[]> baseRequests = new ArrayList<>();
        baseRequests.add(baseRequest);
        setRequestGroupsEditor(proxyModel, baseRequests, networkController, proxyEditor);
    }
}
