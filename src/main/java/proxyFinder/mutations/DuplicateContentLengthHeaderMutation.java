package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class DuplicateContentLengthHeaderMutation implements RequestMutationStrategy {
    public String getName() { return "DUPLICATE_CL_HEADER"; }
    public MutationTarget getTarget() { return MutationTarget.HEADER_FULL; }
    public HttpRequest mutate(HttpRequest base) {
        HttpRequest req = base.withRemovedHeader("Content-Length");
        req = req.withAddedHeader("Content-Length", "5");
        req = req.withAddedHeader("Content-Length", "10");
        return req;
    }
}
