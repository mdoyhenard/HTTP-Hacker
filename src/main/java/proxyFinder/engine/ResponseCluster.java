package proxyFinder.engine;

import java.util.*;

public class ResponseCluster {
    public final int statusCode;
    public final String normBody;
    public final Map<String, String> headers; // Only whitelisted headers.
    public final List<ClusterProof> proofs = new ArrayList<>();

    public ResponseCluster(int statusCode, String normBody, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.normBody = normBody;
        this.headers = headers != null ? headers : Collections.emptyMap();
    }

    public void addProof(ClusterProof proof) {
        proofs.add(proof);
    }

    public long medianTiming() {
        if (proofs.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>();
        for (ClusterProof p : proofs) sorted.add(p.timingMillis);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
    }

    public Set<MutationSet> getTriggeringMutations() {
        Set<MutationSet> set = new HashSet<>();
        for (ClusterProof p : proofs) set.add(p.mutationSet);
        return set;
    }

    public boolean matches(int statusCode, String normBody, Map<String, String> otherHeaders) {
        if (this.statusCode != statusCode) return false;
        if (!Objects.equals(this.normBody, normBody)) return false;
        return headersEquivalent(this.headers, otherHeaders);
    }

    private static boolean headersEquivalent(Map<String, String> a, Map<String, String> b) {
        Set<String> keys = HeaderWhitelistConfig.getWhitelist();
        for (String key : keys) {
            String va = a.get(key);
            String vb = b.get(key);
            if (!Objects.equals(va, vb)) return false;
        }
        return true;
    }
}
