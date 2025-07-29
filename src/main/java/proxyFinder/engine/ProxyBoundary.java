package proxyFinder.engine;

import java.util.*;

public class ProxyBoundary {
    public final int layer;
    public final List<ResponseCluster> clusters;
    public final long medianTiming;

    public ProxyBoundary(int layer, List<ResponseCluster> clusters) {
        this.layer = layer;
        this.clusters = clusters;
        this.medianTiming = computeMedianTiming(clusters);
    }

    private static long computeMedianTiming(List<ResponseCluster> clusters) {
        List<Long> all = new ArrayList<>();
        for (ResponseCluster c : clusters) {
            all.add(c.medianTiming());
        }
        if (all.isEmpty()) return 0;
        Collections.sort(all);
        int n = all.size();
        if (n % 2 == 1) return all.get(n / 2);
        return (all.get(n / 2 - 1) + all.get(n / 2)) / 2;
    }
}
