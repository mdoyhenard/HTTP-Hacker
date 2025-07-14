package httpraider.parser;

import httpraider.controller.engines.JSEngine;
import httpraider.model.network.HttpParserModel;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class ParserUtils {

    public static byte[] decodeEscapedSequence(String s) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                if (next == 'r') { result.append('\r'); i += 2; }
                else if (next == 'n') { result.append('\n'); i += 2; }
                else if (next == 't') { result.append('\t'); i += 2; }
                else if (next == '\\') { result.append('\\'); i += 2; }
                else { result.append(next); i += 2; }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    public static String decodeEscapedSequenceStr(String s) {
        return new String(decodeEscapedSequence(s), StandardCharsets.ISO_8859_1);
    }

    private static int indexOf(byte[] data, byte[] pattern, int start) {
        outer:
        for (int i = start; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
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

    public static ParserResult splitHeaders(HttpParserModel model, byte[] data) {
        List<String> seqs = model.getHeaderLineEndings();
        int splitIndex = -1;
        byte[] foundSeq = null;
        for (int i = 0; i < seqs.size(); i++) {
            for (int j = 0; j < seqs.size(); j++) {
                byte[] seq1 = decodeEscapedSequence(seqs.get(i));
                byte[] seq2 = decodeEscapedSequence(seqs.get(j));
                byte[] combined = new byte[seq1.length + seq2.length];
                System.arraycopy(seq1, 0, combined, 0, seq1.length);
                System.arraycopy(seq2, 0, combined, seq1.length, seq2.length);
                int idx = indexOf(data, combined, 0);
                if (idx >= 0 && (splitIndex == -1 || idx < splitIndex)) {
                    splitIndex = idx;
                    foundSeq = combined;
                }
            }
        }
        if (splitIndex == -1 || foundSeq == null) {
            return new ParserResult(data, new byte[0], "Header/body delimiter not found");
        }
        int end = splitIndex + foundSeq.length;
        byte[] headers = Arrays.copyOfRange(data, 0, end);
        byte[] rest = Arrays.copyOfRange(data, end, data.length);
        return new ParserResult(headers, rest, null);
    }

    public static List<String> splitHeaderLines(HttpParserModel model, byte[] headersBytes) {
        String headers = new String(headersBytes, StandardCharsets.ISO_8859_1);
        List<String> seqs = model.getHeaderLineEndings();
        if (seqs == null || seqs.isEmpty()) {
            List<String> l = new ArrayList<>();
            l.add(headers);
            return l;
        }
        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < seqs.size(); i++) {
            if (i > 0) patternBuilder.append("|");
            patternBuilder.append(Pattern.quote(decodeEscapedSequenceStr(seqs.get(i))));
        }
        Pattern pattern = Pattern.compile("(.*?(?:" + patternBuilder + "))", Pattern.DOTALL);

        List<String> result = new ArrayList<>();
        Matcher matcher = pattern.matcher(headers);
        int end = 0;
        while (matcher.find()) {
            result.add(matcher.group(1));
            end = matcher.end();
        }
        if (end < headers.length()) {
            result.add(headers.substring(end));
        }

        return result;
    }

    public static String[] splitRequestLineSimultaneous(String requestLine, List<String> delimiters) {
        if (delimiters == null || delimiters.isEmpty()) return null;
        List<DelimiterMatch> matches = new ArrayList<>();
        for (String delim : delimiters) {
            String delimStr = decodeEscapedSequenceStr(delim);
            int idx = requestLine.indexOf(delimStr);
            while (idx >= 0) {
                matches.add(new DelimiterMatch(idx, delimStr.length()));
                idx = requestLine.indexOf(delimStr, idx + 1);
            }
        }
        if (matches.size() < 2) return null;
        matches.sort(Comparator.comparingInt(a -> a.position));
        DelimiterMatch first = matches.get(0);
        DelimiterMatch second = null;
        for (DelimiterMatch m : matches) {
            if (m.position > first.position) {
                second = m;
                break;
            }
        }
        if (second == null) return null;
        int a = first.position;
        int b = second.position;
        String part1 = requestLine.substring(0, a);
        String part2 = requestLine.substring(a + first.length, b);
        String part3 = requestLine.substring(b + second.length);
        return new String[] { part1, part2, part3 };
    }

    private static class DelimiterMatch {
        int position, length;
        DelimiterMatch(int p, int l) { position = p; length = l; }
    }

    public static List<String> runHeaderLinesJs(HttpParserModel model, List<String> headerLines) {
        if (!model.isUseHeaderLinesJs()) return headerLines;
        try {
            return JSEngine.runSplitHeaderScript(headerLines, mergeStringsToBytes(headerLines), model.getHeaderLinesScript());
        } catch (Exception e) {
            throw new RuntimeException("HeaderLines JS Error: " + e);
        }
    }

    public static String runRequestLineJs(HttpParserModel model, String requestLine) {
        if (!model.isUseRequestLineJs()) return requestLine;
        try {
            String out = JSEngine.runTagEngine(model.getRequestLineScript(), requestLine);
            return out == null ? requestLine : out;
        } catch (Exception e) {
            throw new RuntimeException("RequestLine JS Error: " + e);
        }
    }

    public static MessageLengthHeaderResult getMessageBodyByHeaderRules(HttpParserModel model, List<String> headerLines, byte[] body) {
        List<HttpParserModel.BodyLenHeaderRule> rules = model.getBodyLenHeaderRules();
        if (rules == null || rules.isEmpty()) {
            return new MessageLengthHeaderResult(headerLines, new byte[0], body, null, 0, null);
        }
        Map<String, List<String>> headerMap = new LinkedHashMap<>();
        for (String line : headerLines) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                String name = line.substring(0, idx).trim().toLowerCase();
                String value = line.substring(idx + 1).trim();
                headerMap.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
            }
        }
        int bodyLen = 0;
        String error = null;
        for (HttpParserModel.BodyLenHeaderRule rule : rules) {
            String pattern = rule.getPattern();
            if (pattern == null || pattern.trim().isEmpty()) continue;
            String headerName = pattern.replace(":", "").trim().toLowerCase();
            if (headerMap.containsKey(headerName)) {
                List<String> values = headerMap.get(headerName);
                if (values.size() > 1) {
                    switch (rule.getDuplicateHandling()) {
                        case FIRST:
                            try {
                                bodyLen = Integer.parseInt(values.get(0));
                            } catch (Exception e) {
                                error = "Invalid value for header: " + headerName;
                            }
                            break;
                        case LAST:
                            try {
                                bodyLen = Integer.parseInt(values.get(values.size() - 1));
                            } catch (Exception e) {
                                error = "Invalid value for header: " + headerName;
                            }
                            break;
                        case ERROR:
                            error = "Duplicate header: " + headerName;
                            break;
                    }
                } else {
                    try {
                        bodyLen = Integer.parseInt(values.get(0));
                    } catch (Exception e) {
                        error = "Invalid value for header: " + headerName;
                    }
                }
                if (error != null) {
                    return new MessageLengthHeaderResult(headerLines, new byte[0], body, error, 0, null);
                }
                if (bodyLen < 0) {
                    return new MessageLengthHeaderResult(headerLines, new byte[0], body, "Body length is negative", 0, null);
                }
                if (bodyLen > body.length) {
                    int missing = bodyLen - body.length;
                    byte[] partial = Arrays.copyOfRange(body, 0, body.length);
                    return new MessageLengthHeaderResult(headerLines, partial, new byte[0], null, missing, null);
                }
                byte[] reqBody = Arrays.copyOfRange(body, 0, bodyLen);
                byte[] remaining = Arrays.copyOfRange(body, bodyLen, body.length);
                return new MessageLengthHeaderResult(headerLines, reqBody, remaining, null, 0, null);
            }
        }
        // No relevant header found, treat as Content-Length 0
        return new MessageLengthHeaderResult(headerLines, new byte[0], body, null, 0, null);
    }

    public static class HeaderLinesBodyEncodingResult {
        public final List<String> headerLines;
        public final byte[] body;
        public HeaderLinesBodyEncodingResult(List<String> h, byte[] b) {
            this.headerLines = h;
            this.body = b;
        }
    }

    public static HeaderLinesBodyEncodingResult runMessageLengthJs(HttpParserModel model, List<String> headerLines, byte[] body, byte[] buffer) {
        if (!model.isUseMessageLengthJs()) return new HeaderLinesBodyEncodingResult(headerLines, body);
        try {
            byte[][] res = JSEngine.runBodyLenScript(headerLines, body, buffer, model.getMessageLengthScript());
            List<String> outHeaderLines = new ArrayList<>(headerLines);
            if (res.length > 1 && res[1] != null) {
                String h = new String(res[1], StandardCharsets.ISO_8859_1);
                outHeaderLines = List.of(h.split("\\r?\\n"));
            }
            return new HeaderLinesBodyEncodingResult(outHeaderLines, res[0]);
        } catch (Exception e) {
            throw new RuntimeException("MessageLength JS Error: " + e);
        }
    }

    public static byte[] mergeStringsToBytes(List<String> strings) {
        StringBuilder sb = new StringBuilder();
        for (String s : strings) sb.append(s);
        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
