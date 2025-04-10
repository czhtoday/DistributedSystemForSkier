package com.upic.model;

/**
 * Represents a skier's lift ride event.
 * This model captures essential information about a lift ride,
 * including the time of the ride and the lift identifier.
 */
public class LiftRide {
  private int time;   // The time at which the lift ride occurred (valid range: 1-360 seconds)
  private int liftID; // The unique identifier of the lift taken (valid range: 1-40)

  /**
   * Gets the time of the lift ride.
   *
   * @return The time in seconds (between 1 and 360).
   */
  public int getTime() {
    return time;
  }

  /**
   * Sets the time of the lift ride.
   *
   * @param time The time in seconds (must be between 1 and 360).
   *            If an invalid value is provided, it may cause request rejection.
   */
  public void setTime(int time) {
    this.time = time;
  }

  /**
   * Gets the lift ID.
   *
   * @return The lift ID (between 1 and 40).
   */
  public int getLiftID() {
    return liftID;
  }

  /**
   * Sets the lift ID.
   *
   * @param liftID The unique lift identifier (must be between 1 and 40).
   *              Providing an invalid lift ID may result in request failure.
   */
  public void setLiftID(int liftID) {
    this.liftID = liftID;
  }
}
