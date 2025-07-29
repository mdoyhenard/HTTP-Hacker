package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class InvalidBodyMutation implements RequestMutationStrategy {
    public String getName() { return "INVALID_BODY"; }
    public MutationTarget getTarget() { return MutationTarget.BODY; }
    public HttpRequest mutate(HttpRequest base) {
        return base.withBody("\0\0\0<script>alert(XSS)");
    }
}
