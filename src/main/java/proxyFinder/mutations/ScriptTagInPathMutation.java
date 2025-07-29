package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class ScriptTagInPathMutation implements RequestMutationStrategy {
    public String getName() { return "SCRIPT_TAG_PATH"; }
    public MutationTarget getTarget() { return MutationTarget.PATH; }
    public HttpRequest mutate(HttpRequest base) {
        return base.withPath(base.path() + "?a=<script>alert('XSS');eval()</script>");
    }
}
