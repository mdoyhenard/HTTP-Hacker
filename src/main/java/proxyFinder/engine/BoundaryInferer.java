package proxyFinder.engine;

import java.util.*;

public class BoundaryInferer {

    /**
     * Main entry: takes a list of clusters and infers the ordered proxy boundary chain.
     * @param clusters All unique error clusters (from clustering).
     * @return Ordered list of ProxyBoundary objects (outermost to innermost).
     */
    public List<ProxyBoundary> inferBoundaries(List<ResponseCluster> clusters) {
        if (clusters == null || clusters.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Group clusters by similar median timing (within a tolerance)
        List<List<ResponseCluster>> grouped = groupClustersByTiming(clusters, 20); // 20ms tolerance

        // 2. Refine with mutation sets: merge clusters into same boundary if always triggered together (stub for now)
        List<ProxyBoundary> boundaries = new ArrayList<>();
        int layerNum = 1;
        for (List<ResponseCluster> group : grouped) {
            // If you later want more merging logic, do it here
            boundaries.add(new ProxyBoundary(layerNum++, group));
        }

        return boundaries;
    }

    private List<List<ResponseCluster>> groupClustersByTiming(List<ResponseCluster> clusters, int toleranceMs) {
        List<ResponseCluster> sorted = new ArrayList<>(clusters);
        sorted.sort(Comparator.comparingLong(ResponseCluster::medianTiming));

        List<List<ResponseCluster>> grouped = new ArrayList<>();
        List<ResponseCluster> current = new ArrayList<>();
        long prevTiming = -1;
        for (ResponseCluster c : sorted) {
            long timing = c.medianTiming();
            if (current.isEmpty() || Math.abs(timing - prevTiming) <= toleranceMs) {
                current.add(c);
            } else {
                grouped.add(new ArrayList<>(current));
                current.clear();
                current.add(c);
            }
            prevTiming = timing;
        }
        if (!current.isEmpty()) grouped.add(current);
        return grouped;
    }
}
