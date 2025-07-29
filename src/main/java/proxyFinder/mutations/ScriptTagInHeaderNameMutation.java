package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class ScriptTagInHeaderNameMutation implements RequestMutationStrategy {
    public String getName() { return "SCRIPT_TAG_HEADER_NAME"; }
    public MutationTarget getTarget() { return MutationTarget.HEADER_NAME; }
    public HttpRequest mutate(HttpRequest base) {
        return base.withAddedHeader("abc<script>def", "value");
    }
}
