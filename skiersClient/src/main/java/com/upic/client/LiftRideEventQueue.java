package com.upic.client;

import io.swagger.client.model.LiftRide;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A thread-safe queue for storing LiftRide events.
 * This class uses a {@link BlockingQueue} to manage the storage and retrieval of lift ride events
 * in a producer-consumer model, ensuring efficient handling in a multi-threaded environment.
 */
public class LiftRideEventQueue {
  // Maximum queue capacity to prevent excessive memory usage
  private static final int QUEUE_CAPACITY = 10000;

  /**
   * A thread-safe queue to store LiftRide events.
   * This queue allows multiple producer threads to add events and multiple consumer threads to retrieve events.
   * The {@link LinkedBlockingQueue} is chosen because:
   * - It provides blocking behavior, meaning producers will wait if the queue is full, and consumers will wait if it's empty.
   * - It prevents excessive memory consumption by limiting the queue size.
   * - It is backed by a linked structure, which dynamically manages memory more efficiently than an array-based queue.
   */
  private static final BlockingQueue<LiftRideEvent> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

  /**
   * Adds a new LiftRide event to the queue.
   * This method is a producer that creates a {@link LiftRideEvent} and inserts it into the queue.
   * If the queue is full, the {@link BlockingQueue#put(Object)} method will block until space becomes available.
   *
   * @param liftRide  The lift ride details.
   * @param skierID   The unique identifier of the skier.
   * @param resortID  The unique identifier of the resort.
   * @param seasonID  The season ID in which the event occurred.
   * @param dayID     The day ID within the season.
   * @throws InterruptedException If the thread is interrupted while waiting for space in the queue.
   */
  public static void addEvent(LiftRide liftRide, int skierID, int resortID, String seasonID, String dayID) throws InterruptedException {
    queue.put(new LiftRideEvent(liftRide, skierID, resortID, seasonID, dayID)); // Blocks if the queue is full.
  }

  /**
   * Retrieves and removes the next LiftRide event from the queue.
   * This method is a consumer that retrieves an event from the queue.
   * If the queue is empty, the {@link BlockingQueue#take()} method will block until an event is available.
   *
   * @return The next LiftRideEvent from the queue.
   * @throws InterruptedException If the thread is interrupted while waiting for an event.
   */
  public static LiftRideEvent pollEvent() throws InterruptedException {
    return queue.take(); // Blocks if the queue is empty.
  }

  /**
   * Checks whether the queue is empty.
   *
   * @return {@code true} if the queue contains no elements, otherwise {@code false}.
   */
  public static boolean isEmpty() {
    return queue.isEmpty();
  }
}
