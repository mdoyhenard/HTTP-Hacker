package proxyFinder.engine;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.HttpRequestResponse;
import proxyFinder.mutations.RequestMutationStrategy;

import java.util.ArrayList;
import java.util.List;

public class MutationRunner {

    private final MontoyaApi api;
    private final int sampleCount;
    private final int maxCombinationSize;
    private final boolean enableSuppression;

    public MutationRunner(MontoyaApi api, int sampleCount, int maxCombinationSize, boolean enableSuppression) {
        this.api = api;
        this.sampleCount = sampleCount;
        this.maxCombinationSize = maxCombinationSize;
        this.enableSuppression = enableSuppression;
    }

    public List<MutationRunResult> runAll(HttpRequest base, List<RequestMutationStrategy> mutations) {
        List<MutationSet> allSets = enumerateMutationSets(mutations, maxCombinationSize, enableSuppression);
        List<MutationRunResult> results = new ArrayList<MutationRunResult>();
        for (MutationSet mset : allSets) {
            HttpRequest mutated = mset.apply(base);
            List<HttpRequestResponse> samples = new ArrayList<HttpRequestResponse>();
            List<Long> timings = new ArrayList<Long>();
            for (int i = 0; i < sampleCount; i++) {
                long start = System.nanoTime();
                HttpRequestResponse resp = null;
                try {
                    resp = api.http().sendRequest(mutated);
                } catch (Throwable t) {}
                long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
                if (resp != null && resp.response() != null) {
                    samples.add(resp);
                    timings.add(elapsedMillis);
                }
            }
            if (!samples.isEmpty()) {
                results.add(new MutationRunResult(mset, mutated, samples, timings));
            }
        }
        return results;
    }

    public static List<MutationSet> enumerateMutationSets(List<RequestMutationStrategy> mutations, int maxCombinationSize, boolean enableSuppression) {
        List<MutationSet> allSets = new ArrayList<MutationSet>();
        int n = mutations.size();
        for (RequestMutationStrategy m : mutations) {
            allSets.add(new MutationSet(java.util.Arrays.asList(m)));
        }
        for (int size = 2; size <= maxCombinationSize && size <= n; size++) {
            List<List<RequestMutationStrategy>> combos = new ArrayList<List<RequestMutationStrategy>>();
            collectCombinations(mutations, size, 0, new ArrayList<RequestMutationStrategy>(), combos);
            for (List<RequestMutationStrategy> combo : combos) {
                allSets.add(new MutationSet(combo));
                if (enableSuppression && size > 1) {
                    for (int omit = 0; omit < combo.size(); omit++) {
                        List<RequestMutationStrategy> suppressed = new ArrayList<RequestMutationStrategy>(combo);
                        suppressed.remove(omit);
                        allSets.add(new MutationSet(suppressed));
                    }
                }
            }
        }
        // Deduplicate
        java.util.Set<String> seen = new java.util.HashSet<String>();
        List<MutationSet> uniqueSets = new ArrayList<MutationSet>();
        for (MutationSet set : allSets) {
            String key = set.describe();
            if (seen.add(key)) {
                uniqueSets.add(set);
            }
        }
        return uniqueSets;
    }

    private static void collectCombinations(List<RequestMutationStrategy> mutations, int size, int start, List<RequestMutationStrategy> current, List<List<RequestMutationStrategy>> result) {
        if (current.size() == size) {
            result.add(new ArrayList<RequestMutationStrategy>(current));
            return;
        }
        for (int i = start; i < mutations.size(); i++) {
            current.add(mutations.get(i));
            collectCombinations(mutations, size, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
