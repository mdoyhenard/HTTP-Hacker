package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class InvalidHeaderValueMutation implements RequestMutationStrategy {
    public String getName() { return "INVALID_HEADER_VALUE"; }
    public MutationTarget getTarget() { return MutationTarget.HEADER_VALUE; }
    public HttpRequest mutate(HttpRequest base) {
        return base.withAddedHeader("X-Test", "bad\0value");
    }
}
