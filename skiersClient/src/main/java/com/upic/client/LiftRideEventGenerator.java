package com.upic.client;

import io.swagger.client.model.LiftRide;

import java.util.Random;

/**
 * A utility class for generating random LiftRide events.
 * This class provides methods to generate random lift ride details,
 * skier IDs, and resort IDs for testing or simulation purposes.
 */
public class LiftRideEventGenerator {
  private static final Random random = new Random();

  public static LiftRide generateRandomLiftRide() {
    LiftRide liftRide = new LiftRide();
    liftRide.setTime(random.nextInt(360) + 1);  // time: 1 - 360
    liftRide.setLiftID(random.nextInt(40) + 1); // liftID: 1 - 40
    return liftRide;
  }

  public static int generateRandomSkierID() {
    return random.nextInt(100000) + 1;  // skierID: 1 - 100000
  }

  public static int generateRandomResortID() {
    return random.nextInt(10) + 1;  // resortID: 1 - 10
  }

  public static final String SEASON_ID = "2025";  // Fixed season
  public static final String DAY_ID = "1";        // Fixed day
}
