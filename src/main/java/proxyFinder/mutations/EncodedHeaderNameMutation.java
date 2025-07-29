package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class EncodedHeaderNameMutation implements RequestMutationStrategy {
    public String getName() { return "ENCODED_HEADER_NAME"; }
    public MutationTarget getTarget() { return MutationTarget.HEADER_NAME; }
    public HttpRequest mutate(HttpRequest base) {
        return base.withAddedHeader("Abc%61def", "value");
    }
}
