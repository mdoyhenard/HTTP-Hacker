package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public interface RequestMutationStrategy {
    String getName();
    MutationTarget getTarget();
    HttpRequest mutate(HttpRequest base);
}