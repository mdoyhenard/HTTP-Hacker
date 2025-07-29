package proxyFinder.engine;

import burp.api.montoya.http.message.HttpRequestResponse;

public class ClusterProof {
    public final MutationSet mutationSet;
    public final HttpRequestResponse response;
    public final long timingMillis;

    public ClusterProof(MutationSet mutationSet, HttpRequestResponse response, long timingMillis) {
        this.mutationSet = mutationSet;
        this.response = response;
        this.timingMillis = timingMillis;
    }
}
