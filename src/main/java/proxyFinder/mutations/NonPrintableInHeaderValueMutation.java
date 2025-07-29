package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class NonPrintableInHeaderValueMutation implements RequestMutationStrategy {
    public String getName() { return "NONPRINTABLE_HEADER_VALUE"; }
    public MutationTarget getTarget() { return MutationTarget.HEADER_VALUE; }
    public HttpRequest mutate(HttpRequest base) {
        return base.withAddedHeader("X-Test-NonPrintable", "val\u0001ue");
    }
}
