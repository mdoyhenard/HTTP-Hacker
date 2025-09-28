package proxyFinder.engine;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.HttpRequestResponse;
import proxyFinder.mutations.RequestMutationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class OptimizedMutationRunner {

    private final MontoyaApi api;
    private final int sampleCount;
    private final int maxCombinationSize;
    private final boolean enableSuppression;
    private final int timeoutSeconds;
    private final ExecutorService executor;
    private final Consumer<String> progressCallback;

    public OptimizedMutationRunner(MontoyaApi api, int sampleCount, int maxCombinationSize, 
                                  boolean enableSuppression, int timeoutSeconds,
                                  Consumer<String> progressCallback) {
        this.api = api;
        this.sampleCount = sampleCount;
        this.maxCombinationSize = maxCombinationSize;
        this.enableSuppression = enableSuppression;
        this.timeoutSeconds = timeoutSeconds;
        // Increase thread pool size to prevent deadlock and handle nested tasks
        this.executor = Executors.newCachedThreadPool(); // Dynamic thread pool
        this.progressCallback = progressCallback != null ? progressCallback : s -> {};
    }

    public List<MutationRunResult> runAll(HttpRequest base, List<RequestMutationStrategy> mutations) {
        List<MutationSet> allSets = MutationRunner.enumerateMutationSets(mutations, maxCombinationSize, enableSuppression);
        List<MutationRunResult> results = new ArrayList<>();
        
        progressCallback.accept("Testing " + allSets.size() + " mutation combinations...");
        
        List<Future<MutationRunResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < allSets.size(); i++) {
            final MutationSet mset = allSets.get(i);
            final int index = i;
            
            Future<MutationRunResult> future = executor.submit(() -> {
                progressCallback.accept("Testing mutation " + (index + 1) + "/" + allSets.size() + ": " + mset.describe());
                
                HttpRequest mutated = mset.apply(base);
                List<HttpRequestResponse> samples = new ArrayList<>();
                List<Long> timings = new ArrayList<>();
                
                for (int j = 0; j < sampleCount; j++) {
                    long start = System.nanoTime();
                    HttpRequestResponse resp = null;
                    
                    try {
                        // Create a separate executor for the timeout mechanism to avoid deadlock
                        ExecutorService timeoutExecutor = Executors.newSingleThreadExecutor();
                        try {
                            Future<HttpRequestResponse> requestFuture = timeoutExecutor.submit(() -> 
                                api.http().sendRequest(mutated)
                            );
                            
                            resp = requestFuture.get(timeoutSeconds, TimeUnit.SECONDS);
                        } finally {
                            timeoutExecutor.shutdownNow();
                        }
                    } catch (TimeoutException e) {
                        progressCallback.accept("Request timeout for: " + mset.describe());
                    } catch (Exception e) {
                        progressCallback.accept("Request error for " + mset.describe() + ": " + e.getMessage());
                    }
                    
                    long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
                    
                    if (resp != null && resp.response() != null) {
                        samples.add(resp);
                        timings.add(elapsedMillis);
                        progressCallback.accept("Response received: " + resp.response().statusCode() + 
                                              " in " + elapsedMillis + "ms");
                    } else {
                        progressCallback.accept("No response for mutation: " + mset.describe());
                    }
                }
                
                if (!samples.isEmpty()) {
                    return new MutationRunResult(mset, mutated, samples, timings);
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Collect results with overall timeout
        long overallTimeout = (long) timeoutSeconds * allSets.size() * sampleCount + 60; // Add 60 seconds buffer
        long startTime = System.currentTimeMillis();
        
        for (Future<MutationRunResult> future : futures) {
            try {
                long remainingTime = overallTimeout - (System.currentTimeMillis() - startTime) / 1000;
                if (remainingTime <= 0) {
                    progressCallback.accept("Overall timeout reached, collecting available results...");
                    future.cancel(true);
                    continue;
                }
                
                MutationRunResult result = future.get(remainingTime, TimeUnit.SECONDS);
                if (result != null) {
                    results.add(result);
                }
            } catch (TimeoutException e) {
                progressCallback.accept("Mutation timed out, skipping...");
                future.cancel(true);
            } catch (Exception e) {
                // Ignore other failed mutations
            }
        }
        
        progressCallback.accept("Completed testing " + results.size() + " mutations successfully");
        
        return results;
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}