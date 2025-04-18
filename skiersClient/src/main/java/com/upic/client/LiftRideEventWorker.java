package com.upic.client;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A worker thread responsible for processing LiftRide events and sending HTTP requests to the Skiers API.
 * Each worker retrieves events from {@link LiftRideEventQueue} and submits them via the API.
 *
 * This class supports a rate-limiting mechanism where the first 32 threads are limited to a maximum of
 * {@value #MAX_REQUESTS_PER_THREAD} requests. Additionally, it implements a retry mechanism for handling failed API calls.
 */
public class LiftRideEventWorker implements Runnable {
  private final SkiersApi apiInstance; // API client for sending requests
  private final int threadId;          // Unique identifier for the worker thread
  private final boolean isLimited;     // Whether this thread is rate-limited (applies to first 32 threads)
  private static final int MAX_RETRIES = 5; // Maximum number of retry attempts for failed requests
  private static final int MAX_REQUESTS_PER_THREAD = 1000; // Max requests allowed for rate-limited threads

  private static int successfulRequests = 0; // Count of successfully processed requests
  private static int failedRequests = 0;     // Count of failed requests

  /**
   * A thread-safe queue that logs request details.
   * Each entry follows the format: "timestamp,HTTP method,latency,response code".
   */
  public static final ConcurrentLinkedQueue<String> requestLog = new ConcurrentLinkedQueue<>();

  /**
   * Constructs a LiftRideEventWorker.
   *
   * @param threadId  The unique ID of this worker thread.
   * @param isLimited Whether this thread is subject to request limitations.
   */
  public LiftRideEventWorker(int threadId, boolean isLimited) {
    this.threadId = threadId;
    this.isLimited = isLimited;
    ApiClient apiClient = new ApiClient();
    // Uncomment the following line for local testing
//    apiClient.setBasePath("http://54.245.205.23:8080"); // My EC2 Spring Boot deployment
//    apiClient.setBasePath("http://54.245.205.23:8080/skiers-server-Servlet_war"); // My EC2 Servlet deployment
    apiClient.setBasePath("http://54.218.63.141:8080/skiersServer");
//    apiClient.setBasePath("http://44.247.192.247:8080/skiersServer_war");
//    apiClient.setBasePath("http://skiers-load-balancer-89222294.us-west-2.elb.amazonaws.com/skiersServer_war");




    // Uncomment the following line for local testing
//    apiClient.setBasePath("http://localhost:8080/skiers_server_Servlet_war_exploded"); // My Local testing
//    apiClient.setBasePath("http://localhost:8080/skiersServer_war_exploded"); // local testing for assignment2
    this.apiInstance = new SkiersApi(apiClient);
  }

  /**
   * Executes the worker thread logic:
   * - Retrieves events from the queue.
   * - Sends the event data to the API.
   * - Implements retry logic for failed requests.
   * - Logs request details.
   */
  @Override
  public void run() {
    int requestCount = 0;

    // Process events while the queue is not empty
    while (!LiftRideEventQueue.isEmpty()) {
      if (isLimited && requestCount >= MAX_REQUESTS_PER_THREAD) {
        break; // Exit if this thread is limited and has processed 1000 requests
      }

      try {
        LiftRideEvent event = LiftRideEventQueue.pollEvent();
        if (event == null) {
          break; // Exit if no more events are available
        }

        boolean success = false;
        int attempts = 0;

        // Attempt to send the request, retrying if necessary
        while (!success && attempts < MAX_RETRIES) {
          long startTime = System.currentTimeMillis(); // Record request start time
          try {
            // Send API request
            apiInstance.writeNewLiftRide(event.getLiftRide(),
                event.getResortID(),
                event.getSeasonID(),
                event.getDayID(),
                event.getSkierID());
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;

            // Update success count in a thread-safe manner
            synchronized (LiftRideEventWorker.class) {
              successfulRequests++;
            }

            // Log successful request
            requestLog.add(String.format("%d,POST,%d,201", startTime, latency));
            success = true; // Mark request as successfully processed
          } catch (ApiException e) {
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            attempts++; // Increment retry attempt counter

            // If all retry attempts fail, log as a failed request
            if (attempts >= MAX_RETRIES) {
              synchronized (LiftRideEventWorker.class) {
                failedRequests++;
              }
              requestLog.add(String.format("%d,POST,%d,%d", startTime, latency, e.getCode()));
            }
          }
        }

        requestCount++; // Increment processed request count for rate-limited threads

      } catch (Exception e) {
        System.err.println("Thread " + threadId + " encountered an error: " + e.getMessage());
      }
    }
  }

  /**
   * Retrieves the count of successfully processed requests.
   *
   * @return The number of successful requests.
   */
  public static synchronized int getSuccessfulRequests() {
    return successfulRequests;
  }

  /**
   * Retrieves the count of failed requests.
   *
   * @return The number of failed requests.
   */
  public static synchronized int getFailedRequests() {
    return failedRequests;
  }
}
