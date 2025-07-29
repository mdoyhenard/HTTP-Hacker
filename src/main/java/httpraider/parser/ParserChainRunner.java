package httpraider.parser;

import httpraider.controller.engines.JSEngine;
import httpraider.model.network.*;
import httpraider.controller.NetworkController;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ParserChainRunner {

    public static List<List<byte[]>> parseOnlyCurrentProxyForPanel(ProxyModel currentProxy, byte[] payload) {
        List<byte[]> requests = parseRequestsForProxyRaw(currentProxy.getParserSettings(), payload);
        List<List<byte[]>> groups = new ArrayList<>();
        if (!requests.isEmpty()) {
            groups.add(requests);
        }
        return groups;
    }

    public static List<List<byte[]>> parseFinalGroupsForPanel(
            ProxyModel currentProxy,
            byte[] payload,
            NetworkController networkController
    ) {
        List<ProxyModel> chain = networkController.getConnectionPathToClient(currentProxy.getId());
        if (chain == null) chain = new ArrayList<>();

        List<byte[]> currentGroup = new ArrayList<>();
        currentGroup.add(payload);

        for (int i = 0; i < chain.size(); i++) {
            ProxyModel proxy = chain.get(i);
            
            // Get all forward connections for this proxy (excluding client)
            List<ProxyModel> forwardConnections = networkController.getDirectConnections(proxy.getId())
                .stream()
                .filter(p -> !p.isClient())
                .collect(Collectors.toList());
            
            // Remove connections that lead back towards client (to avoid loops)
            if (i > 0) {
                ProxyModel previousProxy = chain.get(i - 1);
                forwardConnections.removeIf(p -> p.getId().equals(previousProxy.getId()));
            }
            
            List<LoadBalancingRule> rules = proxy.getParserSettings().getLoadBalancingRules();
            
            // Check if there are enabled rules
            boolean hasEnabledRules = false;
            if (rules != null) {
                for (LoadBalancingRule rule : rules) {
                    if (rule.isEnabled()) {
                        hasEnabledRules = true;
                        break;
                    }
                }
            }

            List<byte[]> nextGroup = new ArrayList<>();
            for (byte[] data : currentGroup) {
                List<byte[]> validRequests = parseValidRequestsForProxyRaw(proxy.getParserSettings(), data);

                if (forwardConnections.size() == 1 && !hasEnabledRules) {
                    // Single forward connection and no rules: forward everything to that connection
                    nextGroup.addAll(validRequests);
                } else if (forwardConnections.size() > 1 || hasEnabledRules) {
                    // Multiple connections or has rules: use rules to determine forwarding
                    for (byte[] req : validRequests) {
                        // Check rules against all possible forward connections
                        for (ProxyModel forwardProxy : forwardConnections) {
                            if (!hasEnabledRules || matchesForwardingRule(rules, req, forwardProxy.getId())) {
                                nextGroup.add(req);
                                break; // Forward to first matching proxy only
                            }
                        }
                    }
                }
                // If forwardConnections.size() == 0, nothing is forwarded (dead end)
            }
            currentGroup = nextGroup;
        }

        List<List<byte[]>> finalGroups = new ArrayList<>();
        for (byte[] data : currentGroup) {
            List<byte[]> requests = parseRequestsForProxyRaw(currentProxy.getParserSettings(), data);
            finalGroups.add(requests);
        }
        return finalGroups;
    }



    public static List<byte[]> parseRequestsForProxyRaw(HttpParserModel model, byte[] data) {
        List<byte[]> results = new ArrayList<>();
        byte[] remaining = data;

        while (remaining.length > 0) {
            List<String> headerLineEndings = model.getHeaderLineEndings();
            if (headerLineEndings == null || headerLineEndings.isEmpty()) {
                String tag = "<parsing_error:\"No header line endings were configured\">";
                results.add(concat(tag.getBytes(StandardCharsets.ISO_8859_1), remaining));
                break;
            }

            ParserResult split = ParserUtils.splitHeaders(model, remaining);
            byte[] headersBytes = split.getParsedPayload();
            byte[] rest = split.getUnparsedRemaining();
            String headerErr = split.getError();

            if (headersBytes.length == 0 && headerErr != null && headerErr.contains("Header/body delimiter not found")) {
                results.add(concat(remaining, "<incomplete_request: incomplete headers block>".getBytes(StandardCharsets.ISO_8859_1)));
                break;
            }
            if (headersBytes.length == 0 || (headerErr != null && !headerErr.contains("Header/body delimiter not found"))) {
                String tag = "<parsing_error:\"" + (headerErr != null ? headerErr : "Header section not found") + "\">";
                results.add(concat(tag.getBytes(StandardCharsets.ISO_8859_1), remaining));
                break;
            }

            // 1. Split header lines and fold (before any other step)
            List<String> headerLines = ParserUtils.splitHeaderLines(model, headersBytes);
            if (model.isAllowHeaderFolding()) {
                headerLines = foldHeaderLines(headerLines, headerLineEndings);
            }

            // 2. Delete header rules
            List<String> deleteRules = model.getDeleteHeaderRules();
            if (deleteRules != null && !deleteRules.isEmpty()) {
                headerLines.removeIf(line -> {
                    for (String match : deleteRules) {
                        if (match != null && !match.isEmpty() && line.contains(match)) {
                            return true;
                        }
                    }
                    return false;
                });
            }

            // 3. Add header rules (insert before the header-end block)
            List<String> addRules = model.getAddHeaderRules();
            if (addRules != null && !addRules.isEmpty()) {
                String addEnding = getBestHeaderLineEnding(headerLineEndings);
                int insertAt = headerLines.size();
                for (int i = headerLines.size() - 1; i >= 0; i--) {
                    if (headerLines.get(i).trim().isEmpty()) {
                        insertAt = i;
                    } else {
                        break;
                    }
                }
                for (String add : addRules) {
                    if (add != null && !add.isEmpty()) {
                        headerLines.add(insertAt, add + addEnding);
                        insertAt++;
                    }
                }
            }

            // 4. JS transformations
            if (model.isUseHeaderLinesJs()) {
                try {
                    headerLines = ParserUtils.runHeaderLinesJs(model, headerLines);
                } catch (Exception ex) {
                    String tag = "<parsing_error:\"" + ex.getMessage() + "\">";
                    results.add(concat(tag.getBytes(StandardCharsets.ISO_8859_1), remaining));
                    break;
                }
            }

            // 5. Request line logic (method rewrite, version, decode)
            List<String> requestLineDelimiters = model.getRequestLineDelimiters();
            if (requestLineDelimiters == null || requestLineDelimiters.isEmpty()) {
                String tag = "<parsing_error:\"No request line delimiters were configured\">";
                results.add(concat(tag.getBytes(StandardCharsets.ISO_8859_1), remaining));
                break;
            }

            String method = null, uri = null, version = null;
            boolean requestLineError = false;
            String requestLineErrorMsg = null;
            if (!headerLines.isEmpty()) {
                String requestLine = headerLines.get(0).trim();

                if (model.isUseRequestLineJs()) {
                    try {
                        requestLine = ParserUtils.runRequestLineJs(model, requestLine);
                    } catch (Exception ex) {
                        String tag = "<parsing_error:\"" + ex.getMessage() + "\">";
                        results.add(concat(tag.getBytes(StandardCharsets.ISO_8859_1), remaining));
                        break;
                    }
                }

                String[] parts = ParserUtils.splitRequestLineSimultaneous(requestLine, requestLineDelimiters);
                if (parts == null || parts.length != 3) {
                    requestLineError = true;
                    requestLineErrorMsg = "Invalid request line: could not split into exactly 3 parts";
                } else {
                    method = parts[0];
                    uri = parts[1];
                    version = parts[2];

                    if (model.isRewriteMethodEnabled()) {
                        String from = model.getFromMethod();
                        String to = model.getToMethod();
                        if (method != null && from != null && !from.isEmpty()
                                && to != null && !to.isEmpty()
                                && method.equalsIgnoreCase(from)) {
                            method = to;
                        }
                    }

                    if (model.isDecodeUrlBeforeForwarding() && uri != null) {
                        String fromRange = model.getUrlDecodeFrom();
                        String toRange = model.getUrlDecodeTo();
                        if (fromRange != null && toRange != null) {
                            try {
                                int fromInt = Integer.parseInt(fromRange.replace("%", ""), 16);
                                int toInt = Integer.parseInt(toRange.replace("%", ""), 16);
                                StringBuilder decoded = new StringBuilder();
                                for (int i = 0; i < uri.length(); ) {
                                    if (uri.charAt(i) == '%' && i + 2 < uri.length()) {
                                        String hex = uri.substring(i + 1, i + 3);
                                        int val = Integer.parseInt(hex, 16);
                                        if (val >= fromInt && val <= toInt) {
                                            decoded.append((char) val);
                                            i += 3;
                                            continue;
                                        }
                                    }
                                    decoded.append(uri.charAt(i));
                                    i++;
                                }
                                uri = decoded.toString();
                            } catch (Exception ignored) {}
                        }
                    }

                    if (model.getForcedHttpVersion() != null) {
                        switch (model.getForcedHttpVersion()) {
                            case HTTP_1_0: version = "HTTP/1.0"; break;
                            case HTTP_1_1: version = "HTTP/1.1"; break;
                            case AUTO: break;
                        }
                    }
                    String oldEnding = "";
                    String origLine = headerLines.get(0);
                    for (String ending : headerLineEndings) {
                        String endingDecoded = ParserUtils.decodeEscapedSequenceStr(ending);
                        if (origLine.endsWith(endingDecoded)) {
                            oldEnding = endingDecoded;
                            break;
                        }
                    }
                    headerLines.set(0, method + " " + uri + " " + version + oldEnding);
                }
            } else {
                requestLineError = true;
                requestLineErrorMsg = "Request line missing";
            }

            if (requestLineError) {
                String tag = "<parsing_error:\"" + requestLineErrorMsg + "\">";
                results.add(concat(tag.getBytes(StandardCharsets.ISO_8859_1), remaining));
                break;
            }

            List<String> currentHeaderLines = new ArrayList<>(headerLines);
            byte[] currentBody = rest;
            byte[] buffer = rest;

            MessageLengthHeaderResult lenResult = ParserUtils.getMessageBodyByHeaderRules(model, currentHeaderLines, currentBody);

            if (lenResult != null && lenResult.getIncompleteBodyBytes() > 0 && lenResult.getChunkedIncompleteTag() == null) {
                byte[] partial = Arrays.copyOfRange(remaining, 0, headersBytes.length + Math.min(lenResult.getBody().length, currentBody.length));
                String tag = "<incomplete_request: " + lenResult.getIncompleteBodyBytes() + " body bytes missing>";
                results.add(concat(partial, tag.getBytes(StandardCharsets.ISO_8859_1)));
                break;
            }
            if (lenResult != null && lenResult.getChunkedIncompleteTag() != null) {
                byte[] partial = Arrays.copyOfRange(remaining, 0, headersBytes.length + lenResult.getBody().length);
                String tag = "<incomplete_request: chunks incomplete>";
                results.add(concat(tag.getBytes(StandardCharsets.ISO_8859_1), remaining));
                break;
            }
            if (lenResult.getError() != null) {
                String tag = "<parsing_error:\"" + lenResult.getError() + "\">";
                results.add(concat(tag.getBytes(StandardCharsets.ISO_8859_1), remaining));
                break;
            }

            List<String> afterJsHeaderLines = currentHeaderLines;
            byte[] afterJsBody = lenResult.getBody();

            if (model.isUseMessageLengthJs()) {
                try {
                    ParserUtils.HeaderLinesBodyEncodingResult jsResult = ParserUtils.runMessageLengthJs(model, afterJsHeaderLines, afterJsBody, buffer);
                    afterJsHeaderLines = jsResult.headerLines;
                    afterJsBody = jsResult.body;
                } catch (Exception ex) {
                    String tag = "<parsing_error:\"" + ex.getMessage() + "\">";
                    results.add(concat(tag.getBytes(StandardCharsets.ISO_8859_1), remaining));
                    break;
                }
            }

            // --- Build header block as-is (no extra endings), preserving all endings and structure
            StringBuilder headerBlock = new StringBuilder();
            for (String line : afterJsHeaderLines) {
                headerBlock.append(line);
            }
            byte[] headerBytes = headerBlock.toString().getBytes(StandardCharsets.ISO_8859_1);
            byte[] bodyBytes = afterJsBody;
            byte[] rawRequest = new byte[headerBytes.length + bodyBytes.length];
            System.arraycopy(headerBytes, 0, rawRequest, 0, headerBytes.length);
            System.arraycopy(bodyBytes, 0, rawRequest, headerBytes.length, bodyBytes.length);

            byte[] afterBody = lenResult.getRemaining();

            // Check firewall rules before adding the request
            FirewallCheckResult firewallResult = checkFirewallRules(model, rawRequest, afterJsHeaderLines, afterJsBody);
            if (firewallResult.blocked) {
                // Add the blocked request with WAF tags
                String wafTag = "<WAF_RULE: the request was not forwarded as it hit a rule for \"" + 
                               firewallResult.triggeredSource + "\">";
                if (firewallResult.closeConnection) {
                    wafTag += "<CONNECTION_CLOSED_BY_WAF>";
                }
                results.add(concat(rawRequest, wafTag.getBytes(StandardCharsets.ISO_8859_1)));
                
                // If connection should be closed, stop processing further requests
                if (firewallResult.closeConnection) {
                    break;
                }
                // Continue processing next request if connection not closed
                remaining = afterBody;
                if (remaining.length > 0) {
                    continue;
                } else {
                    break;
                }
            }

            boolean incomplete = false;
            if (headersBytes.length == 0 || headerErr != null) {
                incomplete = true;
            }

            if (!incomplete && afterBody.length == 0) {
                results.add(rawRequest);
                break;
            }

            if (afterBody.length > 0) {
                results.add(rawRequest);
                remaining = afterBody;
                continue;
            }

            if (incomplete) {
                results.add(concat(rawRequest, "<incomplete_request>".getBytes(StandardCharsets.ISO_8859_1)));
                break;
            }

            results.add(rawRequest);
            break;
        }
        return results;
    }

    public static List<byte[]> parseValidRequestsForProxyRaw(HttpParserModel model, byte[] data) {
        List<byte[]> valid = new ArrayList<>();
        byte[] remaining = data;

        while (remaining.length > 0) {
            List<String> headerLineEndings = model.getHeaderLineEndings();
            if (headerLineEndings == null || headerLineEndings.isEmpty()) break;

            ParserResult split = ParserUtils.splitHeaders(model, remaining);
            byte[] headersBytes = split.getParsedPayload();
            byte[] rest = split.getUnparsedRemaining();
            String headerErr = split.getError();

            if (headersBytes.length == 0) break;
            if (headerErr != null) break;

            List<String> headerLines = ParserUtils.splitHeaderLines(model, headersBytes);
            if (model.isAllowHeaderFolding()) {
                headerLines = foldHeaderLines(headerLines, headerLineEndings);
            }

            List<String> deleteRules = model.getDeleteHeaderRules();
            if (deleteRules != null && !deleteRules.isEmpty()) {
                headerLines.removeIf(line -> {
                    for (String match : deleteRules) {
                        if (match != null && !match.isEmpty() && line.contains(match)) {
                            return true;
                        }
                    }
                    return false;
                });
            }

            List<String> addRules = model.getAddHeaderRules();
            if (addRules != null && !addRules.isEmpty()) {
                String addEnding = getBestHeaderLineEnding(headerLineEndings);
                int insertAt = headerLines.size();
                for (int i = headerLines.size() - 1; i >= 0; i--) {
                    if (headerLines.get(i).trim().isEmpty()) {
                        insertAt = i;
                    } else {
                        break;
                    }
                }
                for (String add : addRules) {
                    if (add != null && !add.isEmpty()) {
                        headerLines.add(insertAt, add + addEnding);
                        insertAt++;
                    }
                }
            }

            List<String> requestLineDelimiters = model.getRequestLineDelimiters();
            if (requestLineDelimiters == null || requestLineDelimiters.isEmpty()) break;

            if (model.isUseHeaderLinesJs()) {
                try {
                    headerLines = ParserUtils.runHeaderLinesJs(model, headerLines);
                } catch (Exception ex) {
                    break;
                }
            }

            String method = null, uri = null, version = null;
            boolean requestLineError = false;
            if (!headerLines.isEmpty()) {
                String requestLine = headerLines.get(0).trim();
                if (model.isUseRequestLineJs()) {
                    try {
                        requestLine = ParserUtils.runRequestLineJs(model, requestLine);
                    } catch (Exception ex) {
                        break;
                    }
                }
                String[] parts = ParserUtils.splitRequestLineSimultaneous(requestLine, requestLineDelimiters);
                if (parts == null || parts.length != 3) {
                    requestLineError = true;
                } else {
                    method = parts[0];
                    uri = parts[1];
                    version = parts[2];

                    if (model.isRewriteMethodEnabled()) {
                        String from = model.getFromMethod();
                        String to = model.getToMethod();
                        if (method != null && from != null && !from.isEmpty()
                                && to != null && !to.isEmpty()
                                && method.equalsIgnoreCase(from)) {
                            method = to;
                        }
                    }
                    if (model.isDecodeUrlBeforeForwarding() && uri != null) {
                        String fromRange = model.getUrlDecodeFrom();
                        String toRange = model.getUrlDecodeTo();
                        if (fromRange != null && toRange != null) {
                            try {
                                int fromInt = Integer.parseInt(fromRange.replace("%", ""), 16);
                                int toInt = Integer.parseInt(toRange.replace("%", ""), 16);
                                StringBuilder decoded = new StringBuilder();
                                for (int i = 0; i < uri.length(); ) {
                                    if (uri.charAt(i) == '%' && i + 2 < uri.length()) {
                                        String hex = uri.substring(i + 1, i + 3);
                                        int val = Integer.parseInt(hex, 16);
                                        if (val >= fromInt && val <= toInt) {
                                            decoded.append((char) val);
                                            i += 3;
                                            continue;
                                        }
                                    }
                                    decoded.append(uri.charAt(i));
                                    i++;
                                }
                                uri = decoded.toString();
                            } catch (Exception ignored) {}
                        }
                    }
                    if (model.getForcedHttpVersion() != null) {
                        switch (model.getForcedHttpVersion()) {
                            case HTTP_1_0: version = "HTTP/1.0"; break;
                            case HTTP_1_1: version = "HTTP/1.1"; break;
                            case AUTO: break;
                        }
                    }
                    String oldEnding = "";
                    String origLine = headerLines.get(0);
                    for (String ending : headerLineEndings) {
                        String endingDecoded = ParserUtils.decodeEscapedSequenceStr(ending);
                        if (origLine.endsWith(endingDecoded)) {
                            oldEnding = endingDecoded;
                            break;
                        }
                    }
                    headerLines.set(0, method + " " + uri + " " + version + oldEnding);
                }
            } else {
                requestLineError = true;
            }
            if (requestLineError) break;

            List<String> currentHeaderLines = new ArrayList<>(headerLines);
            byte[] currentBody = rest;
            byte[] buffer = rest;

            MessageLengthHeaderResult lenResult = ParserUtils.getMessageBodyByHeaderRules(model, currentHeaderLines, currentBody);

            if (lenResult != null && (lenResult.getIncompleteBodyBytes() > 0 && lenResult.getChunkedIncompleteTag() == null)) break;
            if (lenResult != null && lenResult.getChunkedIncompleteTag() != null) break;
            if (lenResult.getError() != null) break;

            List<String> afterJsHeaderLines = currentHeaderLines;
            byte[] afterJsBody = lenResult.getBody();

            if (model.isUseMessageLengthJs()) {
                try {
                    ParserUtils.HeaderLinesBodyEncodingResult jsResult = ParserUtils.runMessageLengthJs(model, afterJsHeaderLines, afterJsBody, buffer);
                    afterJsHeaderLines = jsResult.headerLines;
                    afterJsBody = jsResult.body;
                } catch (Exception ex) {
                    break;
                }
            }

            StringBuilder headerBlock = new StringBuilder();
            for (String line : afterJsHeaderLines) {
                headerBlock.append(line);
            }
            byte[] headerBytes = headerBlock.toString().getBytes(StandardCharsets.ISO_8859_1);
            byte[] bodyBytes = afterJsBody;
            byte[] rawRequest = new byte[headerBytes.length + bodyBytes.length];
            System.arraycopy(headerBytes, 0, rawRequest, 0, headerBytes.length);
            System.arraycopy(bodyBytes, 0, rawRequest, headerBytes.length, bodyBytes.length);

            byte[] afterBody = lenResult.getRemaining();

            // Check firewall rules before adding the request
            FirewallCheckResult firewallResult = checkFirewallRules(model, rawRequest, afterJsHeaderLines, afterJsBody);
            if (firewallResult.blocked) {
                // Don't add blocked requests to valid list
                if (firewallResult.closeConnection) {
                    // Stop processing if connection should be closed
                    break;
                }
                // Continue with next request
                remaining = afterBody;
                if (remaining.length > 0) {
                    continue;
                } else {
                    break;
                }
            }

            boolean incomplete = false;
            if (headersBytes.length == 0 || headerErr != null) {
                incomplete = true;
            }

            if (!incomplete && afterBody.length == 0) {
                valid.add(rawRequest);
                break;
            }

            if (afterBody.length > 0) {
                valid.add(rawRequest);
                remaining = afterBody;
                continue;
            }

            if (incomplete) break;

            valid.add(rawRequest);
            break;
        }
        return valid;
    }

    // RFC folding: line starting with whitespace is appended to previous header line
    private static List<String> foldHeaderLines(List<String> headerLines, List<String> headerLineEndings) {
        List<String> folded = new ArrayList<>();
        for (String line : headerLines) {
            if ((line.length() > 0) && (line.charAt(0) == ' ' || line.charAt(0) == '\t')) {
                // Fold: append to previous, remove one whitespace char at start
                if (!folded.isEmpty()) {
                    String prev = folded.remove(folded.size() - 1);
                    String prevNoEnding = prev;
                    for (String ending : headerLineEndings) {
                        String dec = ParserUtils.decodeEscapedSequenceStr(ending);
                        if (prev.endsWith(dec)) {
                            prevNoEnding = prev.substring(0, prev.length() - dec.length());
                            break;
                        }
                    }
                    String continuation = line.substring(1);

                    folded.add(prevNoEnding + continuation);
                } else {
                    folded.add(line);
                }
            } else {
                folded.add(line);
            }
        }
        return folded;
    }


    // Use best header ending: CRLF if present, else LF if present, else first in list.
    private static String getBestHeaderLineEnding(List<String> endings) {
        if (endings == null || endings.isEmpty()) return "\r\n";
        for (String e : endings) {
            String ending = ParserUtils.decodeEscapedSequenceStr(e);
            if ("\r\n".equals(ending)) return ending;
        }
        for (String e : endings) {
            String ending = ParserUtils.decodeEscapedSequenceStr(e);
            if ("\n".equals(ending)) return ending;
        }
        return ParserUtils.decodeEscapedSequenceStr(endings.get(0));
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
    
    private static class FirewallCheckResult {
        boolean blocked = false;
        boolean closeConnection = false;
        String triggeredSource = "";
    }
    
    private static FirewallCheckResult checkFirewallRules(HttpParserModel model, byte[] rawRequest, 
                                                         List<String> headerLines, byte[] body) {
        FirewallCheckResult result = new FirewallCheckResult();
        
        List<FirewallRule> rules = model.getFirewallRules();
        if (rules == null || rules.isEmpty()) {
            return result;
        }
        
        String requestStr = new String(rawRequest, StandardCharsets.ISO_8859_1);
        
        for (FirewallRule rule : rules) {
            if (!rule.isEnabled()) continue;
            
            String inputValue = null;
            switch (rule.getSource()) {
                case METHOD:
                    if (!headerLines.isEmpty()) {
                        String requestLine = headerLines.get(0);
                        String[] parts = requestLine.split(" ");
                        if (parts.length > 0) {
                            inputValue = parts[0];
                        }
                    }
                    break;
                    
                case URL:
                    if (!headerLines.isEmpty()) {
                        String requestLine = headerLines.get(0);
                        String[] parts = requestLine.split(" ");
                        if (parts.length > 1) {
                            inputValue = parts[1];
                        }
                    }
                    break;
                    
                case VERSION:
                    if (!headerLines.isEmpty()) {
                        String requestLine = headerLines.get(0);
                        String[] parts = requestLine.split(" ");
                        if (parts.length > 2) {
                            // Get the version part (last part before line ending)
                            String lastPart = parts[parts.length - 1];
                            // Remove any line endings
                            inputValue = lastPart.replaceAll("\\r|\\n", "");
                        }
                    }
                    break;
                    
                case HEADERS:
                    // Pass headers as array (excluding request line)
                    if (headerLines.size() > 1) {
                        List<String> headers = new ArrayList<>(headerLines.subList(1, headerLines.size()));
                        // Clean up line endings from headers
                        for (int i = 0; i < headers.size(); i++) {
                            headers.set(i, headers.get(i).replaceAll("\\r|\\n", ""));
                        }
                        // Evaluate with headers array
                        boolean blocked = evaluateFirewallRuleArray(rule.getJsCode(), headers);
                        if (blocked) {
                            result.blocked = true;
                            result.closeConnection = rule.isCloseConnection();
                            result.triggeredSource = rule.getSource().getDisplayName();
                            return result;
                        }
                        continue; // Skip the string evaluation below
                    }
                    break;
                    
                case BODY:
                    inputValue = new String(body, StandardCharsets.ISO_8859_1);
                    break;
                    
                case FULL_REQUEST:
                    inputValue = requestStr;
                    break;
            }
            
            if (inputValue != null && evaluateFirewallRule(rule.getJsCode(), inputValue)) {
                result.blocked = true;
                result.closeConnection = rule.isCloseConnection();
                result.triggeredSource = rule.getSource().getDisplayName();
                return result;
            }
        }
        
        return result;
    }
    
    private static boolean evaluateFirewallRule(String jsCode, String input) {
        return JSEngine.runFirewallRule(jsCode, input);
    }
    
    private static boolean evaluateFirewallRuleArray(String jsCode, List<String> headers) {
        String[] headersArray = headers.toArray(new String[0]);
        return JSEngine.runFirewallRuleArray(jsCode, headersArray);
    }

    private static boolean matchesForwardingRule(List<LoadBalancingRule> rules, byte[] request, String targetProxyId) {
        if (rules == null || rules.isEmpty()) return false;
        for (LoadBalancingRule rule : rules) {
            if (!rule.isEnabled()) continue;
            if (rule.getForwardToProxyId() == null) continue;
            if (!rule.getForwardToProxyId().equals(targetProxyId)) continue;
            if (ruleMatchesRequest(rule, request)) return true;
        }
        return false;
    }


    private static boolean ruleMatchesRequest(httpraider.model.network.LoadBalancingRule rule, byte[] request) {
        String requestStr = new String(request, java.nio.charset.StandardCharsets.ISO_8859_1);
        String[] headerBody = requestStr.split("\r\n\r\n", 2);
        String headersBlock = headerBody.length > 0 ? headerBody[0] : "";
        String body = headerBody.length > 1 ? headerBody[1] : "";

        switch (rule.getRuleType()) {
            case URL:
                String url = extractUrlFromRequestLine(headersBlock);
                if (url == null) return false;
                String urlPattern = rule.getPattern();
                if (urlPattern == null) return false;
                MatchMode matchOpt = rule.getMatchOption();
                if (matchOpt == null) matchOpt = MatchMode.EXACT;
                switch (matchOpt) {
                    case PREFIX: return url.startsWith(urlPattern);
                    case SUFFIX: return url.endsWith(urlPattern);
                    case EXACT:  return url.equals(urlPattern);
                    case ANY_MATCH: return url.contains(urlPattern);
                    default:     return false;
                }
            case HEADERS:
                String headerPattern = rule.getPattern();
                HeaderField field = rule.getHeaderField();
                if (field == HeaderField.NAME_VALUE) {
                    String namePattern = rule.getHeaderNamePattern();
                    String valuePattern = rule.getHeaderValuePattern();
                    MatchMode nameMode = rule.getHeaderNameMatchMode();
                    MatchMode valueMode = rule.getHeaderValueMatchMode();
                    for (String line : headersBlock.split("\r\n")) {
                        int idx = line.indexOf(':');
                        if (idx > 0) {
                            String name = line.substring(0, idx).trim();
                            String value = line.substring(idx + 1).trim();
                            boolean nameMatch = false, valueMatch = false;
                            if (namePattern != null && nameMode != null) {
                                switch (nameMode) {
                                    case PREFIX:    nameMatch = name.startsWith(namePattern); break;
                                    case SUFFIX:    nameMatch = name.endsWith(namePattern); break;
                                    case EXACT:     nameMatch = name.equals(namePattern); break;
                                    case ANY_MATCH: nameMatch = name.contains(namePattern); break;
                                }
                            }
                            if (valuePattern != null && valueMode != null) {
                                switch (valueMode) {
                                    case PREFIX:    valueMatch = value.startsWith(valuePattern); break;
                                    case SUFFIX:    valueMatch = value.endsWith(valuePattern); break;
                                    case EXACT:     valueMatch = value.equals(valuePattern); break;
                                    case ANY_MATCH: valueMatch = value.contains(valuePattern); break;
                                }
                            }
                            if (nameMatch && valueMatch) return true;
                        }
                    }
                    return false;
                } else {
                    MatchMode mode = rule.getMatchMode();
                    for (String line : headersBlock.split("\r\n")) {
                        int idx = line.indexOf(':');
                        String testStr = null;
                        if (field == HeaderField.NAME && idx > 0) testStr = line.substring(0, idx).trim();
                        else if (field == HeaderField.VALUE && idx > 0) testStr = line.substring(idx + 1).trim();
                        else if (field == HeaderField.HEADER_LINE) testStr = line;
                        if (testStr != null) {
                            switch (mode) {
                                case PREFIX:    if (testStr.startsWith(headerPattern)) return true; break;
                                case SUFFIX:    if (testStr.endsWith(headerPattern))   return true; break;
                                case EXACT:     if (testStr.equals(headerPattern))      return true; break;
                                case ANY_MATCH: if (testStr.contains(headerPattern))    return true; break;
                            }
                        }
                    }
                    return false;
                }
            case HOST:
                for (String line : headersBlock.split("\r\n")) {
                    if (line.toLowerCase().startsWith("host:")) {
                        String hostValue = line.substring(5).trim();
                        String pattern = rule.getHostPattern();
                        MatchMode m = rule.getHostMatchMode();
                        if (pattern == null || m == null) continue;
                        switch (m) {
                            case PREFIX:    if (hostValue.startsWith(pattern)) return true; break;
                            case SUFFIX:    if (hostValue.endsWith(pattern))   return true; break;
                            case EXACT:     if (hostValue.equals(pattern))      return true; break;
                            case ANY_MATCH: if (hostValue.contains(pattern))    return true; break;
                        }
                    }
                }
                return false;
            case COOKIES:
                for (String line : headersBlock.split("\r\n")) {
                    if (line.toLowerCase().startsWith("cookie:")) {
                        String cookieStr = line.substring(7).trim();
                        String[] cookies = cookieStr.split(";");
                        for (String cookie : cookies) {
                            String[] kv = cookie.trim().split("=", 2);
                            String cname = kv.length > 0 ? kv[0].trim() : "";
                            String cval = kv.length > 1 ? kv[1].trim() : "";
                            boolean nameMatch = false, valueMatch = false;
                            String namePattern = rule.getCookieNamePattern();
                            String valuePattern = rule.getCookieValuePattern();
                            MatchMode nameMode = rule.getCookieNameMatchMode();
                            MatchMode valueMode = rule.getCookieValueMatchMode();
                            if (namePattern != null && nameMode != null) {
                                switch (nameMode) {
                                    case PREFIX:    nameMatch = cname.startsWith(namePattern); break;
                                    case SUFFIX:    nameMatch = cname.endsWith(namePattern); break;
                                    case EXACT:     nameMatch = cname.equals(namePattern); break;
                                    case ANY_MATCH: nameMatch = cname.contains(namePattern); break;
                                }
                            }
                            if (valuePattern != null && valueMode != null) {
                                switch (valueMode) {
                                    case PREFIX:    valueMatch = cval.startsWith(valuePattern); break;
                                    case SUFFIX:    valueMatch = cval.endsWith(valuePattern); break;
                                    case EXACT:     valueMatch = cval.equals(valuePattern); break;
                                    case ANY_MATCH: valueMatch = cval.contains(valuePattern); break;
                                }
                            }
                            if (nameMatch && valueMatch) return true;
                        }
                    }
                }
                return false;
            case METHOD:
                String[] headerLines = headersBlock.split("\r\n");
                if (headerLines.length == 0) return false;
                String firstLine = headerLines[0];
                String[] methodParts = firstLine.split(" ");
                if (methodParts.length == 0) return false;
                String method = methodParts[0];
                String mPattern = rule.getPattern();
                return mPattern != null && method.equalsIgnoreCase(mPattern);
            case BODY:
                String bodyPattern = rule.getPattern();
                return bodyPattern != null && body.matches(bodyPattern);
            case CUSTOM:
                return evaluateJsRule(rule, headersBlock, body);
            default:
                return false;
        }
    }


    private static String extractUrlFromRequestLine(String headersBlock) {
        if (headersBlock == null) return null;
        String[] lines = headersBlock.split("\r\n", 2);
        if (lines.length == 0) return null;
        String[] reqParts = lines[0].split(" ");
        return reqParts.length > 1 ? reqParts[1] : null;
    }

    private static boolean evaluateJsRule(httpraider.model.network.LoadBalancingRule rule, String headersBlock, String body) {
        String jsCode = rule.getJsCode();
        if (jsCode == null || jsCode.trim().isEmpty()) return false;
        try {
            // Use your JS engine, here we assume you have a static method in JSEngine:
            // boolean result = JSEngine.runRule(jsCode, headersBlock, body);
            // Replace with your JS execution logic:
            Object res = JSEngine.runJsBooleanRule(jsCode, headersBlock, body);
            if (res instanceof Boolean) return (Boolean) res;
            if (res instanceof String) return Boolean.parseBoolean((String) res);
            return false;
        } catch (Exception ex) {
            return false;
        }
    }


}
