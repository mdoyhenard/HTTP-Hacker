package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class UpgradeHeaderMutation implements RequestMutationStrategy {
    public String getName() { return "UPGRADE_HEADER"; }
    public MutationTarget getTarget() { return MutationTarget.HEADER_FULL; }
    public HttpRequest mutate(HttpRequest base) {
        return base.withRemovedHeader("upgrade").withAddedHeader("upgrade", "h2c,websocket");
    }
}
