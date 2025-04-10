package com.upic.client;

import io.swagger.client.model.LiftRide;

/**
 * Represents an event related to a skier's lift ride.
 * This class encapsulates details about a specific lift ride event,
 * including the skier ID, resort ID, season ID, and day ID.
 */
public class LiftRideEvent {
  private final LiftRide liftRide;  // Details of the lift ride event
  private final int skierID;        // Unique identifier for the skier
  private final int resortID;       // Unique identifier for the resort
  private final String seasonID;    // Identifier for the ski season
  private final String dayID;       // Identifier for the specific day of the season

  /**
   * Constructs a new LiftRideEvent with the given parameters.
   *
   * @param liftRide  The lift ride details.
   * @param skierID   The unique ID of the skier.
   * @param resortID  The unique ID of the resort.
   * @param seasonID  The ID of the ski season.
   * @param dayID     The ID of the specific day within the season.
   */
  public LiftRideEvent(LiftRide liftRide, int skierID, int resortID, String seasonID, String dayID) {
    this.liftRide = liftRide;
    this.skierID = skierID;
    this.resortID = resortID;
    this.seasonID = seasonID;
    this.dayID = dayID;
  }

  /**
   * Gets the details of the lift ride.
   *
   * @return The LiftRide object containing ride details.
   */
  public LiftRide getLiftRide() {
    return liftRide;
  }

  /**
   * Gets the skier's unique identifier.
   *
   * @return The skier's ID.
   */
  public int getSkierID() {
    return skierID;
  }

  /**
   * Gets the resort's unique identifier.
   *
   * @return The resort's ID.
   */
  public int getResortID() {
    return resortID;
  }

  /**
   * Gets the season ID for this event.
   *
   * @return The season ID as a string.
   */
  public String getSeasonID() {
    return seasonID;
  }

  /**
   * Gets the day ID for this event.
   *
   * @return The day ID as a string.
   */
  public String getDayID() {
    return dayID;
  }
}
