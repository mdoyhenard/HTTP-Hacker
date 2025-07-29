package proxyFinder.engine;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.HttpRequestResponse;
import java.util.List;

public class MutationRunResult {
    public final MutationSet mutationSet;
    public final HttpRequest mutatedRequest;
    public final List<HttpRequestResponse> responses;
    public final List<Long> timings;

    public MutationRunResult(MutationSet mutationSet, HttpRequest mutatedRequest, List<HttpRequestResponse> responses, List<Long> timings) {
        this.mutationSet = mutationSet;
        this.mutatedRequest = mutatedRequest;
        this.responses = responses;
        this.timings = timings;
    }
}
