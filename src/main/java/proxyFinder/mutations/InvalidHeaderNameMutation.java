package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class InvalidHeaderNameMutation implements RequestMutationStrategy {
    public String getName() { return "INVALID_HEADER_NAME"; }
    public MutationTarget getTarget() { return MutationTarget.HEADER_NAME; }
    public HttpRequest mutate(HttpRequest base) {
        return base.withAddedHeader("X-\0Name", "foo");
    }
}
