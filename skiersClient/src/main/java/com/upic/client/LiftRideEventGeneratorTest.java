package com.upic.client;

import io.swagger.client.model.LiftRide;


/**
 * A test class to verify the functionality of LiftRideEventGenerator.
 * This class generates multiple random LiftRide events and prints their details.
 */
public class LiftRideEventGeneratorTest {
  public static void main(String[] args) {
    // Generate 5 random lift ride events and print them
    for (int i = 0; i < 5; i++) {
      // Generate random lift ride details
      LiftRide liftRide = LiftRideEventGenerator.generateRandomLiftRide();
      int skierID = LiftRideEventGenerator.generateRandomSkierID();
      int resortID = LiftRideEventGenerator.generateRandomResortID();
      String seasonID = LiftRideEventGenerator.SEASON_ID; // Fixed season ID
      String dayID = LiftRideEventGenerator.DAY_ID; // Fixed day ID


      System.out.println("LiftRide Event " + (i + 1) + ":");
      System.out.println("  Skier ID: " + skierID);
      System.out.println("  Resort ID: " + resortID);
      System.out.println("  Season ID: " + seasonID);
      System.out.println("  Day ID: " + dayID);
      System.out.println("  Lift ID: " + liftRide.getLiftID());
      System.out.println("  Time: " + liftRide.getTime());
      System.out.println("----------------------------------");
    }
  }
}
