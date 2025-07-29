package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class NullByteInPathMutation implements RequestMutationStrategy {
    public String getName() { return "NULL_BYTE_PATH"; }
    public MutationTarget getTarget() { return MutationTarget.PATH; }
    public HttpRequest mutate(HttpRequest base) {
        return base.withPath(base.pathWithoutQuery() + "%00abc");
    }
}
