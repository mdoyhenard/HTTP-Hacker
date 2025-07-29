package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class OverlongMethodMutation implements RequestMutationStrategy {
    public String getName() { return "OVERLONG_METHOD"; }
    public MutationTarget getTarget() { return MutationTarget.METHOD; }
    public HttpRequest mutate(HttpRequest base) {
        String overlong = "G" + "E".repeat(400) + "T";
        return base.withMethod(overlong);
    }
}
