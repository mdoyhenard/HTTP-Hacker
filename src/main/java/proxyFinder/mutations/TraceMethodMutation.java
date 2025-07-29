package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class TraceMethodMutation implements RequestMutationStrategy {
    public String getName() { return "TRACE_METHOD"; }
    public MutationTarget getTarget() { return MutationTarget.METHOD; }
    public HttpRequest mutate(HttpRequest base) {
        return base.withMethod("TRACE");
    }
}
