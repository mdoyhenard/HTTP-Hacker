package httpraider.parser;

import httpraider.controller.engines.JSEngine;
import httpraider.model.network.*;
import httpraider.controller.NetworkController;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ParserChainRunner {

    public static List<List<byte[]>> parseOnlyCurrentProxyForPanel(ProxyModel currentProxy, byte[] payload) {
        List<List<byte[]>> groups = new ArrayList<>();
        
        // Client proxy should never parse - just return the raw payload
        if (currentProxy.isClient()) {
            List<byte[]> rawRequest = new ArrayList<>();
            rawRequest.add(payload);
            groups.add(rawRequest);
        } else {
            List<byte[]> requests = parseRequestsForProxyRaw(currentProxy.getParserSettings(), payload);
            if (!requests.isEmpty()) {
                groups.add(requests);
            }
        }
        return groups;
    }

    public static List<List<byte[]>> parseFinalGroupsForPanel(
            ProxyModel currentProxy,
            byte[] payload,
            NetworkController networkController
    ) {
        // Find the client proxy to start from
        ProxyModel clientProxy = null;
        for (ProxyModel proxy : networkController.getModel().getProxies()) {
            if (proxy.isClient()) {
                clientProxy = proxy;
                break;
            }
        }
        
        if (clientProxy == null) {
            // No client proxy found, parse directly
            List<List<byte[]>> groups = new ArrayList<>();
            List<byte[]> requests = parseRequestsForProxyRaw(currentProxy.getParserSettings(), payload);
            if (!requests.isEmpty()) {
                groups.add(requests);
            }
            return groups;
        }

        // BFS to process all paths from client
        // Map: proxyId -> list of byte arrays (requests) that reached this proxy
        Map<String, List<byte[]>> proxyPayloads = new HashMap<>();
        proxyPayloads.put(clientProxy.getId(), Arrays.asList(payload));
        
        // Queue for BFS: contains proxy IDs to process
        Queue<String> toProcess = new LinkedList<>();
        toProcess.add(clientProxy.getId());
        Set<String> processed = new HashSet<>();
        
        while (!toProcess.isEmpty()) {
            String proxyId = toProcess.poll();
            if (processed.contains(proxyId)) continue;
            processed.add(proxyId);
            
            ProxyModel proxy = networkController.getModel().getProxy(proxyId);
            if (proxy == null) continue;
            
            List<byte[]> payloadsForThisProxy = proxyPayloads.get(proxyId);
            if (payloadsForThisProxy == null || payloadsForThisProxy.isEmpty()) continue;
            
            // Get forward connections (excluding client)
            List<ProxyModel> forwardConnections = networkController.getDirectConnections(proxyId)
                .stream()
                .filter(p -> !p.isClient())
                .collect(Collectors.toList());
            
            if (forwardConnections.isEmpty()) continue;
            
            // Check if proxy has enabled rules
            List<LoadBalancingRule> rules = proxy.getParserSettings().getLoadBalancingRules();
            boolean hasEnabledRules = false;
            if (rules != null) {
                for (LoadBalancingRule rule : rules) {
                    if (rule.isEnabled()) {
                        hasEnabledRules = true;
                        break;
                    }
                }
            }
            
            // Process each payload through this proxy
            for (byte[] data : payloadsForThisProxy) {
                List<byte[]> allRequests;
                
                // Client doesn't parse - it just forwards the raw payload
                if (proxy.isClient()) {
                    allRequests = Arrays.asList(data);
                } else {
                    // Parse requests but DON'T forward incomplete for intermediate proxies
                    allRequests = parseRequestsForProxyRaw(proxy.getParserSettings(), data, false);
                }
                
                if (forwardConnections.size() == 1 && !hasEnabledRules) {
                    // Single forward connection and no rules: forward everything
                    ProxyModel nextProxy = forwardConnections.get(0);
                    // Only add if not already processed or queued to prevent cycles
                    if (!processed.contains(nextProxy.getId()) && !toProcess.contains(nextProxy.getId())) {
                        proxyPayloads.computeIfAbsent(nextProxy.getId(), k -> new ArrayList<>())
                                    .addAll(allRequests);
                        toProcess.add(nextProxy.getId());
                    }
                } else if (hasEnabledRules) {
                    // Has rules: check each request against rules
                    for (byte[] req : allRequests) {
                        boolean forwarded = false;
                        
                        // Check rules for this request
                        for (LoadBalancingRule rule : rules) {
                            if (!rule.isEnabled()) continue;
                            String targetProxyId = rule.getForwardToProxyId();
                            if (targetProxyId == null) continue;
                            
                            // Check if target is in forward connections
                            ProxyModel targetProxy = forwardConnections.stream()
                                .filter(p -> p.getId().equals(targetProxyId))
                                .findFirst()
                                .orElse(null);
                            
                            if (targetProxy != null && ruleMatchesRequest(rule, req)) {
                                // Only add if not already processed or queued to prevent cycles
                                if (!processed.contains(targetProxyId) && !toProcess.contains(targetProxyId)) {
                                    proxyPayloads.computeIfAbsent(targetProxyId, k -> new ArrayList<>())
                                                .add(req);
                                    toProcess.add(targetProxyId);
                                }
                                forwarded = true;
                                break; // First matching rule wins
                            }
                        }
                        
                        // If no rule matched, don't forward (when rules are enabled)
                    }
                } else if (forwardConnections.size() > 1) {
                    // Multiple connections but no rules: this is likely a configuration error
                    // Forward to first connection only to avoid duplication
                    ProxyModel nextProxy = forwardConnections.get(0);
                    // Only add if not already processed or queued to prevent cycles
                    if (!processed.contains(nextProxy.getId()) && !toProcess.contains(nextProxy.getId())) {
                        proxyPayloads.computeIfAbsent(nextProxy.getId(), k -> new ArrayList<>())
                                    .addAll(allRequests);
                        toProcess.add(nextProxy.getId());
                    }
                }
            }
        }
        
        // Get the payloads that reached the target proxy
        List<byte[]> targetPayloads = proxyPayloads.getOrDefault(currentProxy.getId(), new ArrayList<>());
        
        // Parse the final requests for display (unless it's the client proxy)
        List<List<byte[]>> finalGroups = new ArrayList<>();
        if (currentProxy.isClient()) {
            // Client proxy should never parse - just return the raw payloads
            for (byte[] data : targetPayloads) {
                List<byte[]> rawGroup = new ArrayList<>();
                rawGroup.add(data);
                finalGroups.add(rawGroup);
            }
        } else {
            // Non-client proxies: concatenate all payloads to allow cross-pipeline completion
            if (!targetPayloads.isEmpty()) {
                // Calculate total size
                int totalSize = 0;
                for (byte[] targetPayload : targetPayloads) {
                    totalSize += targetPayload.length;
                }
                
                // Concatenate all payloads into one continuous stream
                byte[] concatenatedData = new byte[totalSize];
                int offset = 0;
                for (byte[] targetPayload : targetPayloads) {
                    System.arraycopy(targetPayload, 0, concatenatedData, offset, targetPayload.length);
                    offset += targetPayload.length;
                }
                
                // Parse the concatenated data as one continuous stream
                List<byte[]> requests = parseRequestsForProxyRaw(currentProxy.getParserSettings(), concatenatedData, false);
                if (!requests.isEmpty()) {
                    finalGroups.add(requests);
                }
            }
        }
        
        return finalGroups;
    }



    public static List<byte[]> parseRequestsForProxyRaw(HttpParserModel model, byte[] data) {
        return parseRequestsForProxyRaw(model, data, true);
    }
    
    // Internal method with option to include incomplete requests (for testing) or exclude them (for forwarding)
    private static List<byte[]> parseRequestsForProxyRaw(HttpParserModel model, byte[] data, boolean includeIncomplete) {
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
                // Headers are incomplete
                if (includeIncomplete) {
                    // For testing: mark as incomplete
                    String tag = "<incomplete_request:incomplete headers>";
                    results.add(concat(tag.getBytes(StandardCharsets.ISO_8859_1), remaining));
                }
                // Stop processing this stream - the next data concatenation should complete it
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
                if (parts == null || parts.length != 5) {
                    requestLineError = true;
                    requestLineErrorMsg = "Invalid request line: could not split into exactly 3 parts";
                } else {
                    method = parts[0];
                    uri = parts[1];
                    version = parts[2];
                    String delimiter1 = parts[3];
                    String delimiter2 = parts[4];

                    if (model.isRewriteMethodEnabled()) {
                        String from = model.getFromMethod();
                        String to = model.getToMethod();
                        if (method != null && from != null && !from.isEmpty()
                                && to != null && !to.isEmpty()
                                && method.equals(from)) {
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
                            case AUTO: 
                                String customVersion = model.getCustomHttpVersion();
                                if (customVersion != null && !customVersion.isEmpty()) {
                                    version = customVersion;
                                }
                                break;
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
                    // Use original delimiters instead of hardcoded spaces
                    headerLines.set(0, method + delimiter1 + uri + delimiter2 + version + oldEnding);
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

            // Check if we should preserve original chunked encoding
            boolean preserveChunked = false;
            boolean isChunked = false;
            
            // Check if this is a chunked request
            for (String line : currentHeaderLines) {
                if (line.toLowerCase().contains("transfer-encoding") && line.toLowerCase().contains("chunked")) {
                    isChunked = true;
                    if (model.getOutputBodyEncoding() == HttpParserModel.MessageLenBodyEncoding.DONT_MODIFY) {
                        preserveChunked = true;
                    }
                    break;
                }
            }
            
            MessageLengthHeaderResult lenResult;
            byte[] originalChunkedBody = null;
            
            if (preserveChunked) {
                // For DONT_MODIFY with chunked, we need to preserve the original chunked body
                // Calculate how much of the original body contains the chunked data
                lenResult = ParserUtils.getMessageBodyByHeaderRules(model, currentHeaderLines, currentBody);
                if (lenResult != null && lenResult.getError() == null) {
                    // Calculate the original chunked body size by finding where remaining starts
                    int chunkedBodySize = currentBody.length - lenResult.getRemaining().length;
                    originalChunkedBody = new byte[chunkedBodySize];
                    System.arraycopy(currentBody, 0, originalChunkedBody, 0, chunkedBodySize);
                }
            } else {
                lenResult = ParserUtils.getMessageBodyByHeaderRules(model, currentHeaderLines, currentBody);
            }

            if (lenResult != null && lenResult.getIncompleteBodyBytes() > 0 && lenResult.getChunkedIncompleteTag() == null) {
                // Calculate how much data belongs to this incomplete request
                int consumedBytes = headersBytes.length + lenResult.getBody().length;
                
                // Check if we can complete this request with the remaining data
                int missingBytes = lenResult.getIncompleteBodyBytes();
                byte[] afterBody = lenResult.getRemaining();
                
                if (afterBody.length >= missingBytes) {
                    // We have enough data to complete the request!
                    byte[] completedBody = new byte[lenResult.getBody().length + missingBytes];
                    System.arraycopy(lenResult.getBody(), 0, completedBody, 0, lenResult.getBody().length);
                    System.arraycopy(afterBody, 0, completedBody, lenResult.getBody().length, missingBytes);
                    
                    // Update lenResult with the completed body
                    lenResult = new MessageLengthHeaderResult(
                        currentHeaderLines,  // headerLines
                        completedBody,       // body
                        Arrays.copyOfRange(afterBody, missingBytes, afterBody.length), // remaining
                        null,               // error
                        0,                  // incompleteBodyBytes (now complete)
                        null                // chunkedIncompleteTag
                    );
                    // Continue processing with the completed request
                } else {
                    // Incomplete request
                    if (includeIncomplete) {
                        // For testing: mark as incomplete with details about missing bytes
                        String incompleteTag = "<incomplete_request:" + missingBytes + " body bytes missing>";
                        byte[] tagBytes = incompleteTag.getBytes(StandardCharsets.ISO_8859_1);
                        
                        // Build the header block
                        StringBuilder headerBlock = new StringBuilder();
                        for (String line : currentHeaderLines) {
                            headerBlock.append(line);
                        }
                        byte[] headerBytes = headerBlock.toString().getBytes(StandardCharsets.ISO_8859_1);
                        
                        // Build the incomplete request with tag
                        ByteArrayOutputStream incompleteRequest = new ByteArrayOutputStream();
                        try {
                            incompleteRequest.write(tagBytes);
                            incompleteRequest.write(headerBytes);
                            incompleteRequest.write(lenResult.getBody());
                        } catch (Exception e) {
                            // Shouldn't happen with ByteArrayOutputStream
                        }
                        results.add(incompleteRequest.toByteArray());
                    }
                    
                    // Try to find more complete requests after it
                    byte[] skipData = lenResult.getRemaining();
                    
                    if (skipData.length > 0) {
                        // Try to find the next valid request start
                        // Look for a valid HTTP method in the remaining data
                        String remainingStr = new String(skipData, StandardCharsets.ISO_8859_1);
                        
                        // Common HTTP methods to look for
                        String[] httpMethods = {"GET ", "POST ", "PUT ", "DELETE ", "HEAD ", "OPTIONS ", "PATCH ", "CONNECT ", "TRACE "};
                        int nextRequestStart = -1;
                        
                        for (String httpMethod : httpMethods) {
                            int idx = remainingStr.indexOf(httpMethod);
                            if (idx >= 0 && (nextRequestStart == -1 || idx < nextRequestStart)) {
                                nextRequestStart = idx;
                            }
                        }
                        
                        if (nextRequestStart > 0) {
                            // Found a potential next request, skip to it
                            remaining = Arrays.copyOfRange(skipData, nextRequestStart, skipData.length);
                            continue;
                        } else if (nextRequestStart == 0) {
                            // Next request starts immediately
                            remaining = skipData;
                            continue;
                        }
                    }
                    
                    // No more data to process
                    break;
                }
            }
            // Don't treat chunked encoding as incomplete if we successfully parsed the body
            if (lenResult != null && lenResult.getChunkedIncompleteTag() != null && lenResult.getBody().length == 0) {
                // Only mark as incomplete if we couldn't parse any body
                byte[] partial = Arrays.copyOfRange(remaining, 0, headersBytes.length);
                String tag = "<incomplete_request: chunks incomplete>";
                results.add(concat(partial, tag.getBytes(StandardCharsets.ISO_8859_1)));
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

            // Apply output body encoding transformation
            if (model.getOutputBodyEncoding() != null && model.getOutputBodyEncoding() != HttpParserModel.MessageLenBodyEncoding.DONT_MODIFY) {
                afterJsHeaderLines = applyOutputBodyEncoding(model, afterJsHeaderLines, afterJsBody, headerLineEndings);
                afterJsBody = transformBodyEncoding(model, afterJsBody);
            } else if (preserveChunked && originalChunkedBody != null) {
                // Use the original chunked body instead of the decoded body
                afterJsBody = originalChunkedBody;
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
                if (parts == null || parts.length != 5) {
                    requestLineError = true;
                } else {
                    method = parts[0];
                    uri = parts[1];
                    version = parts[2];
                    String delimiter1 = parts[3];
                    String delimiter2 = parts[4];

                    if (model.isRewriteMethodEnabled()) {
                        String from = model.getFromMethod();
                        String to = model.getToMethod();
                        if (method != null && from != null && !from.isEmpty()
                                && to != null && !to.isEmpty()
                                && method.equals(from)) {
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
                            case AUTO: 
                                String customVersion = model.getCustomHttpVersion();
                                if (customVersion != null && !customVersion.isEmpty()) {
                                    version = customVersion;
                                }
                                break;
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
                    headerLines.set(0, method + delimiter1 + uri + delimiter2 + version + oldEnding);
                }
            } else {
                requestLineError = true;
            }
            if (requestLineError) break;

            List<String> currentHeaderLines = new ArrayList<>(headerLines);
            byte[] currentBody = rest;
            byte[] buffer = rest;

            // Check if we should preserve original chunked encoding
            boolean preserveChunked = false;
            boolean isChunked = false;
            
            // Check if this is a chunked request
            for (String line : currentHeaderLines) {
                if (line.toLowerCase().contains("transfer-encoding") && line.toLowerCase().contains("chunked")) {
                    isChunked = true;
                    if (model.getOutputBodyEncoding() == HttpParserModel.MessageLenBodyEncoding.DONT_MODIFY) {
                        preserveChunked = true;
                    }
                    break;
                }
            }
            
            MessageLengthHeaderResult lenResult;
            byte[] originalChunkedBody = null;
            
            if (preserveChunked) {
                // For DONT_MODIFY with chunked, we need to preserve the original chunked body
                lenResult = ParserUtils.getMessageBodyByHeaderRules(model, currentHeaderLines, currentBody);
                if (lenResult != null && lenResult.getError() == null) {
                    // Calculate the original chunked body size
                    int chunkedBodySize = currentBody.length - lenResult.getRemaining().length;
                    originalChunkedBody = new byte[chunkedBodySize];
                    System.arraycopy(currentBody, 0, originalChunkedBody, 0, chunkedBodySize);
                }
            } else {
                lenResult = ParserUtils.getMessageBodyByHeaderRules(model, currentHeaderLines, currentBody);
            }

            if (lenResult != null && (lenResult.getIncompleteBodyBytes() > 0 && lenResult.getChunkedIncompleteTag() == null)) break;
            // Only break if chunked parsing failed (no body parsed)
            if (lenResult != null && lenResult.getChunkedIncompleteTag() != null && lenResult.getBody().length == 0) break;
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
            
            // Apply output body encoding transformation or preserve chunked
            if (model.getOutputBodyEncoding() != null && model.getOutputBodyEncoding() != HttpParserModel.MessageLenBodyEncoding.DONT_MODIFY) {
                afterJsHeaderLines = applyOutputBodyEncoding(model, afterJsHeaderLines, afterJsBody, headerLineEndings);
                afterJsBody = transformBodyEncoding(model, afterJsBody);
            } else if (preserveChunked && originalChunkedBody != null) {
                // Use the original chunked body instead of the decoded body
                afterJsBody = originalChunkedBody;
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

                    folded.add(prevNoEnding + " " + continuation);
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
    
    private static List<String> applyOutputBodyEncoding(HttpParserModel model, List<String> headerLines, 
                                                        byte[] body, List<String> headerLineEndings) {
        List<String> modifiedHeaders = new ArrayList<>();
        String lineEnding = getBestHeaderLineEnding(headerLineEndings);
        boolean hasTransferEncoding = false;
        boolean hasContentLength = false;
        
        // Copy headers, removing conflicting headers based on output encoding
        for (String line : headerLines) {
            String lowerLine = line.toLowerCase();
            if (lowerLine.startsWith("transfer-encoding:")) {
                hasTransferEncoding = true;
                if (model.getOutputBodyEncoding() == HttpParserModel.MessageLenBodyEncoding.FORCE_CL_HEADER) {
                    continue; // Skip Transfer-Encoding when forcing Content-Length
                }
            } else if (lowerLine.startsWith("content-length:")) {
                hasContentLength = true;
                if (model.getOutputBodyEncoding() == HttpParserModel.MessageLenBodyEncoding.FORCE_CHUNKED) {
                    continue; // Skip Content-Length when forcing chunked
                }
            }
            modifiedHeaders.add(line);
        }
        
        // Add appropriate header based on output encoding
        if (model.getOutputBodyEncoding() == HttpParserModel.MessageLenBodyEncoding.FORCE_CHUNKED) {
            if (!hasTransferEncoding) {
                // Find position to insert (before empty line)
                int insertPos = modifiedHeaders.size();
                for (int i = modifiedHeaders.size() - 1; i >= 0; i--) {
                    if (modifiedHeaders.get(i).trim().isEmpty()) {
                        insertPos = i;
                        break;
                    }
                }
                modifiedHeaders.add(insertPos, "Transfer-Encoding: chunked" + lineEnding);
            }
        } else if (model.getOutputBodyEncoding() == HttpParserModel.MessageLenBodyEncoding.FORCE_CL_HEADER) {
            if (!hasContentLength) {
                // Find position to insert (before empty line)
                int insertPos = modifiedHeaders.size();
                for (int i = modifiedHeaders.size() - 1; i >= 0; i--) {
                    if (modifiedHeaders.get(i).trim().isEmpty()) {
                        insertPos = i;
                        break;
                    }
                }
                modifiedHeaders.add(insertPos, "Content-Length: " + body.length + lineEnding);
            }
        }
        
        return modifiedHeaders;
    }
    
    private static byte[] transformBodyEncoding(HttpParserModel model, byte[] body) {
        if (model.getOutputBodyEncoding() == HttpParserModel.MessageLenBodyEncoding.FORCE_CHUNKED) {
            // Convert to chunked encoding
            ByteArrayOutputStream chunked = new ByteArrayOutputStream();
            try {
                // Write body as single chunk
                String sizeHex = Integer.toHexString(body.length);
                chunked.write(sizeHex.getBytes(StandardCharsets.ISO_8859_1));
                chunked.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
                chunked.write(body);
                chunked.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
                // Write final chunk
                chunked.write("0\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
                return chunked.toByteArray();
            } catch (Exception e) {
                return body; // Return original on error
            }
        }
        // For FORCE_CL_HEADER or DONT_MODIFY, return body as-is
        // (body is already decoded from chunked if it was chunked)
        return body;
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
