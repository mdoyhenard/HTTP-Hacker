package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class CLandTEMutation implements RequestMutationStrategy {
    public String getName() { return "CL_AND_TE"; }
    public MutationTarget getTarget() { return MutationTarget.HEADER_FULL; }
    public HttpRequest mutate(HttpRequest base) {
        HttpRequest req = base.withRemovedHeader("Content-Length").withRemovedHeader("Transfer-Encoding").withBody("0\r\n\r\n");
        req = req.withAddedHeader("Content-Length", "5");
        req = req.withAddedHeader("Transfer-Encoding", "chunked");
        return req;
    }
}
