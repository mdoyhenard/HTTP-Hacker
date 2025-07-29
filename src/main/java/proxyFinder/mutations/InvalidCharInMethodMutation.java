package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class InvalidCharInMethodMutation implements RequestMutationStrategy {
    public String getName() { return "INVALID_CHAR_METHOD"; }
    public MutationTarget getTarget() { return MutationTarget.METHOD; }
    public HttpRequest mutate(HttpRequest base) {
        String method = base.method();
        if (method.length() > 1) method = method.substring(0, 1) + "#" + method.substring(1);
        else method = method + "#";
        return base.withMethod(method);
    }
}
