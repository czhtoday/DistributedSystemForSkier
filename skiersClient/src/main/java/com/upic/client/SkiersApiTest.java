package com.upic.client;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

/**
 * A simple test client to verify the connectivity and functionality of the Skiers API.
 *
 * This class:
 * - Initializes an API client.
 * - Configures the base path for either local testing or an EC2 deployment.
 * - Constructs a sample LiftRide event with valid parameters.
 * - Sends a POST request to record the lift ride.
 * - Handles and reports any API exceptions.
 */
public class SkiersApiTest {
  public static void main(String[] args) {
    // Create an API client instance
    ApiClient client = new ApiClient();

    // Set the API base path
    // Uncomment the following line for local testing
//    client.setBasePath("http://localhost:8080/skiers_server_Servlet_war_exploded");
    // Use this line for EC2 deployment testing
//    client.setBasePath("http://54.245.205.23:8080/skiers-server-Servlet_war");
    client.setBasePath("http://54.245.205.23:8080/skiersServer_war");

    // Uncomment the following line for local testing
//    client.setBasePath("http://54.245.205.23:8080"); // My EC2 Spring Boot deployment

    // Create a SkiersApi instance
    SkiersApi apiInstance = new SkiersApi(client);

    // Construct a LiftRide event with valid test data
    LiftRide liftRide = new LiftRide();
    liftRide.setTime(120);  // Time of the ride (valid range: 1-360 seconds)
    liftRide.setLiftID(5);  // Lift ID (valid range: 1-40)

    // Define required parameters for API request
    Integer resortID = 10;   // Resort ID (valid range: 1-10)
    String seasonID = "2025"; // Season ID (String format)
    String dayID = "1";       // Day ID within the season (String format)
    Integer skierID = 123;    // Skier ID (valid range: 1-100000)

    try {
      // Send a POST request to the API
      apiInstance.writeNewLiftRide(liftRide, resortID, seasonID, dayID, skierID);
      System.out.println("✅ API call successful! Lift ride recorded.");
    } catch (ApiException e) {
      // 🔥 Handle API errors gracefully
      System.err.println("❌ API call failed: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
