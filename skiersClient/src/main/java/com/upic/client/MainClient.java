package com.upic.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;

/**
 * The main client class for executing a multi-threaded load test on the LiftRide event system.
 * This class simulates concurrent skier lift ride events and sends them to the server via multiple worker threads.
 *
 * The test consists of two phases:
 * 1. **Initial Phase**: 32 threads, each processing up to 1000 requests (total: 32,000 requests).
 * 2. **Dynamic Phase**: Up to 512 additional threads process the remaining 168,000 requests.
 *
 * The results are recorded, including:
 * - **Total requests processed** (successes and failures).
 * - **Throughput** (requests per second).
 * - **Response time statistics** (mean, median, 99th percentile, min, max).
 * - **Log files** stored in a CSV format.
 */
public class MainClient {
  private static final int NUM_THREADS = 32; // Initial 32 worker threads
  private static final int TOTAL_REQUESTS = 200_000; // Total number of requests to be processed
  private static final int MAX_DYNAMIC_THREADS = 512; // Additional threads for handling remaining requests
  private static final int SINGLE_THREAD_TEST_REQUESTS = 10_000; // Number of single-thread test requests

  public static void main(String[] args) {
    // Step 1: Run Single Thread Test (10,000 requests)
    System.out.println("🚀 Running Single Thread Test (10,000 requests)...");
    List<Long> singleThreadLatencies = new ArrayList<>();

    for (int i = 0; i < SINGLE_THREAD_TEST_REQUESTS; i++) {
      long startTime = System.currentTimeMillis();
      int responseCode = sendSingleRequest(); // Send request
      long latency = System.currentTimeMillis() - startTime;
      singleThreadLatencies.add(latency);
    }

    double avgSingleThreadLatency = singleThreadLatencies.stream().mapToLong(Long::longValue)
        .average().orElse(0);
    double estimatedThroughput = 1000.0 / avgSingleThreadLatency; // Little’s Law

    System.out.println("✅ Single Thread Test Completed!");
    System.out.println(
        "🔹 Avg Latency (Single Thread): " + String.format("%.2f", avgSingleThreadLatency) + " ms");
    System.out.println(
        "⚡ Estimated Max Throughput (Little's Law): " + String.format("%.2f", estimatedThroughput)
            + " req/sec");

    // Step 2: Start Multi-Threaded Load Test
    System.out.println("🚀 Starting Load Test with " + NUM_THREADS + " threads...");

    long startTime = System.currentTimeMillis(); // Record start time

    // Producer thread - Generates and adds events to the queue
    Thread producer = new Thread(() -> {
      for (int i = 0; i < TOTAL_REQUESTS; i++) {
        try {
          LiftRideEventQueue.addEvent(
              LiftRideEventGenerator.generateRandomLiftRide(),
              LiftRideEventGenerator.generateRandomSkierID(),
              LiftRideEventGenerator.generateRandomResortID(),
              LiftRideEventGenerator.SEASON_ID,
              LiftRideEventGenerator.DAY_ID
          );
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          System.err.println("Producer interrupted: " + e.getMessage());
        }
      }
    });
    producer.start();

    // Phase 1: Fixed-size thread pool with 32 threads (each handling up to 1000 requests)
    ExecutorService initialPool = Executors.newFixedThreadPool(NUM_THREADS);
    for (int i = 0; i < NUM_THREADS; i++) {
      initialPool.execute(
          new LiftRideEventWorker(i, true)); // `true` means these threads are rate-limited
    }

    // Phase 2: Dynamic thread pool (handles remaining 168,000 requests)
    System.out.println("🚀 Starting Load Test with " + MAX_DYNAMIC_THREADS + " threads...");
    ExecutorService dynamicPool = Executors.newFixedThreadPool(MAX_DYNAMIC_THREADS);
    for (int i = 0; i < MAX_DYNAMIC_THREADS; i++) {
      dynamicPool.execute(
          new LiftRideEventWorker(NUM_THREADS + i, false)); // `false` means no request limit
    }

    initialPool.shutdown();
    dynamicPool.shutdown();

    try {
      initialPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
      dynamicPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      System.err.println("Executor interrupted: " + e.getMessage());
    }

    long endTime = System.currentTimeMillis(); // Record end time
    long totalTime = endTime - startTime;
    double throughput = (double) TOTAL_REQUESTS / (totalTime / 1000.0);

    // Print test results
    System.out.println("✅ Remaining Load Test Completed!");
    System.out.println(
        "📊 Num of Successful Requests: " + LiftRideEventWorker.getSuccessfulRequests());
    System.out.println("❌ Num of Failed Requests: " + LiftRideEventWorker.getFailedRequests());
    System.out.println("⏳ Total Run Time: " + totalTime + " ms");
    System.out.println("⚡ Throughput: " + String.format("%.2f", throughput) + " requests/sec");

  }

  /**
   * Executes a single API request to test latency.
   * This method is used for single-threaded latency testing before multi-threaded execution.
   *
   * @return HTTP response code (201 if successful, or an error code).
   */
  private static int sendSingleRequest() {
    ApiClient apiClient = new ApiClient();
    // Uncomment the following line for local testing
//    apiClient.setBasePath("http://54.245.205.23:8080"); // My EC2 Spring Boot deployment
//    apiClient.setBasePath("http://54.245.205.23:8080/skiers-server-Servlet_war"); // My EC2 Servlet deployment
    apiClient.setBasePath("http://54.245.205.23:8080/skiersServer_war");
//    apiClient.setBasePath("http://44.247.192.247:8080/skiersServer_war");
//    apiClient.setBasePath("http://skiers-load-balancer-89222294.us-west-2.elb.amazonaws.com/skiersServer_war");



    // Uncomment the following line for local testing
//    apiClient.setBasePath("http://localhost:8080/skiers_server_Servlet_war_exploded"); // My Local testing
    apiClient.setBasePath("http://localhost:8080/skiersServer_war_exploded"); // local testing for assignment2

    SkiersApi apiInstance = new SkiersApi(apiClient);

    try {
      LiftRideEvent event = new LiftRideEvent(
          LiftRideEventGenerator.generateRandomLiftRide(),
          LiftRideEventGenerator.generateRandomSkierID(),
          LiftRideEventGenerator.generateRandomResortID(),
          LiftRideEventGenerator.SEASON_ID,
          LiftRideEventGenerator.DAY_ID
      );

      apiInstance.writeNewLiftRide(event.getLiftRide(), event.getResortID(), event.getSeasonID(), event.getDayID(), event.getSkierID());
      return 201; // HTTP 201 Created
    } catch (ApiException e) {
      return e.getCode(); // Return actual error code for debugging
    }
  }
}
