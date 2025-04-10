package com.upic.queue;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import com.upic.model.LiftRide;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ Consumer that listens to the lift ride queue,
 * processes incoming messages, and stores them in a thread-safe map.
 */
public class MessageQueueConsumer {
  private static final String QUEUE_NAME = "lift_ride_queue";
  private static final String RABBITMQ_HOST = "44.238.46.159"; // RabbitMQ server IP
//  private static final String RABBITMQ_HOST = "localhost"; // Local test, will be replaced with RabbitMQ EC2 address
  private static final String RABBITMQ_USER = "admin";
  private static final String RABBITMQ_PASS = "password123";

  private static final Gson gson = new Gson();

  // Thread-safe HashMap to store skier lift ride records
  private static final Map<Integer, LiftRide> skierLiftRideMap = new ConcurrentHashMap<>();

  public static void main(String[] args) {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(RABBITMQ_HOST);
    factory.setUsername(RABBITMQ_USER);
    factory.setPassword(RABBITMQ_PASS);

    try {
      Connection connection = factory.newConnection();
      Channel channel = connection.createChannel();

      channel.queueDeclare(QUEUE_NAME, true, false, false, null);
      System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

      // Define message consumption logic
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [x] Received '" + message + "'");

        // Parse JSON and store data
        LiftRideMessage liftRideMessage = gson.fromJson(message, LiftRideMessage.class);
        processMessage(liftRideMessage);
      };

      channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });

    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }
  }

  /**
   * Processes the received LiftRideMessage and stores it in the thread-safe HashMap.
   */
  private static void processMessage(LiftRideMessage message) {
    // Construct a LiftRide object
    LiftRide liftRide = new LiftRide();
    liftRide.setTime(message.time);
    liftRide.setLiftID(message.liftID);

    // Store in skierLiftRideMap using skierID as the key
    skierLiftRideMap.put(message.skierID, liftRide);

    System.out.println(" [✔] Stored LiftRide for skierID: " + message.skierID);
  }

  /**
   * Inner class: Used to parse RabbitMQ messages.
   */
  static class LiftRideMessage {
    int resortID;
    String seasonID;
    String dayID;
    int skierID;
    int time;
    int liftID;
  }
}
