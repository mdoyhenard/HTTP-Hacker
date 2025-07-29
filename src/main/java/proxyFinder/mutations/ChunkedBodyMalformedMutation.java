package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class ChunkedBodyMalformedMutation implements RequestMutationStrategy {
    public String getName() { return "MALFORMED_CHUNKED_BODY"; }
    public MutationTarget getTarget() { return MutationTarget.BODY; }
    public HttpRequest mutate(HttpRequest base) {
        String malformed = "ghijk\r\nabcd\r\n0\r\n\r\n";
        return base.withMethod("POST")
                .withRemovedHeader("content-length")
                .withAddedHeader("transfer-encoding", "chunked")
                .withBody(malformed);
    }
}
