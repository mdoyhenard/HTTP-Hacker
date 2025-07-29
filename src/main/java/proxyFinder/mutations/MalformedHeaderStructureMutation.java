package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class MalformedHeaderStructureMutation implements RequestMutationStrategy {
    public String getName() { return "MALFORMED_HEADER_STRUCTURE"; }
    public MutationTarget getTarget() { return MutationTarget.HEADER_FULL; }
    public HttpRequest mutate(HttpRequest base) {
        return base.withAddedHeader(":", "");
    }
}
