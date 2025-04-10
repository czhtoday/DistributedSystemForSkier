package com.upic.queue;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import com.upic.config.RabbitMQConfig;
import com.upic.db.DynamoDBWriter;
import com.upic.model.LiftRide;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Multi-threaded RabbitMQ consumer with batched DynamoDB writes.
 * Thread count reduced to prevent memory overload on t2.micro.
 */
public class MultiThreadedMessageQueueConsumer {
  private static final String QUEUE_NAME = "lift_ride_queue";
  private static final int NUM_CONSUMER_THREADS = 8; // Reduced thread count for memory control
  private static final Gson gson = new Gson();

  public static void main(String[] args) throws Exception {
    Connection connection = RabbitMQConfig.getConnection();

    ExecutorService executor = Executors.newFixedThreadPool(NUM_CONSUMER_THREADS);
    for (int i = 0; i < NUM_CONSUMER_THREADS; i++) {
      executor.submit(new ConsumerWorker(connection));
    }

    System.out.println("[*] Multi-threaded Consumer started with " + NUM_CONSUMER_THREADS + " threads.");
  }

  static class ConsumerWorker implements Runnable {
    private final Connection connection;

    public ConsumerWorker(Connection connection) {
      this.connection = connection;
    }

    @Override
    public void run() {
      try {
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.basicQos(10); // Control prefetch to avoid memory spikes

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");

          try {
            LiftRideMessage liftRideMessage = gson.fromJson(message, LiftRideMessage.class);

            LiftRide liftRide = new LiftRide();
            liftRide.setLiftID(liftRideMessage.liftID);
            liftRide.setTime(liftRideMessage.time);

            // Submit to batch queue
            DynamoDBWriter.writeLiftRide(
                liftRideMessage.skierID,
                liftRideMessage.resortID,
                liftRideMessage.seasonID,
                liftRideMessage.dayID,
                liftRide
            );

            // Minimal log to avoid slowing down threads
            // System.out.println("[✔] Queued skierID: " + liftRideMessage.skierID);

            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
          } catch (Exception e) {
            System.err.println("[!] Error processing message: " + e.getMessage());
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
          }
        };

        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  static class LiftRideMessage {
    int resortID;
    String seasonID;
    String dayID;
    int skierID;
    int time;
    int liftID;
  }
}