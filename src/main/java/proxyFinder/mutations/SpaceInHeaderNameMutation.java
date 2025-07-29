package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class SpaceInHeaderNameMutation implements RequestMutationStrategy {
    public String getName() { return "SPACE_HEADER_NAME"; }
    public MutationTarget getTarget() { return MutationTarget.HEADER_NAME; }
    public HttpRequest mutate(HttpRequest base) {
        return base.withAddedHeader("X Bad", "test");
    }
}
