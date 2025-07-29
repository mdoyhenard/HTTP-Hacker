package proxyFinder.engine;

import burp.api.montoya.http.message.HttpRequestResponse;
import java.util.*;

public class Clusterer {
    public List<ResponseCluster> clusterResponses(List<MutationRunResult> results) {
        List<ResponseCluster> clusters = new ArrayList<>();
        for (MutationRunResult result : results) {
            for (int i = 0; i < result.responses.size(); i++) {
                HttpRequestResponse resp = result.responses.get(i);
                if (resp == null || resp.response() == null) continue;
                int status = resp.response().statusCode();
                String normBody = ResponseNormalizer.normalizeBody(resp.response().bodyToString());
                Map<String, String> headers = toWhitelistedHeaderMap(resp.response().headers());
                long timing = (result.timings != null && result.timings.size() > i) ? result.timings.get(i) : 0;

                boolean matched = false;
                for (ResponseCluster cluster : clusters) {
                    if (cluster.matches(status, normBody, headers)) {
                        cluster.addProof(new ClusterProof(result.mutationSet, resp, timing));
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    ResponseCluster newCluster = new ResponseCluster(status, normBody, headers);
                    newCluster.addProof(new ClusterProof(result.mutationSet, resp, timing));
                    clusters.add(newCluster);
                }
            }
        }
        return clusters;
    }

    private Map<String, String> toWhitelistedHeaderMap(List<burp.api.montoya.http.message.HttpHeader> headers) {
        Map<String, String> map = new TreeMap<>();
        Set<String> whitelist = HeaderWhitelistConfig.getWhitelist();
        if (headers != null) {
            for (var h : headers) {
                String name = h.name() != null ? h.name().toLowerCase().trim() : null;
                if (name != null && whitelist.contains(name)) {
                    map.put(name, h.value() != null ? h.value().trim() : "");
                }
            }
        }
        return map;
    }
}
