package com.upic.queue;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.google.gson.Gson;
import com.upic.model.LiftRide;

public class MessageQueueProducer {
  private static final String QUEUE_NAME = "lift_ride_queue";
  private static final String RABBITMQ_HOST = "44.238.46.159"; // RabbitMQ server IP
  //  private static final String RABBITMQ_USER = "admin"; // RabbitMQ username
//  private static final String RABBITMQ_PASS = "password123"; // RabbitMQ password
//  private static final String RABBITMQ_HOST = "localhost"; // local RabbitMQ
  private static final String RABBITMQ_USER = "admin"; // default user
  private static final String RABBITMQ_PASS = "password123"; // default password

  private static final Gson gson = new Gson();

  private static Connection connection;
  private static Channel channel;

  static {
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(RABBITMQ_HOST);
      factory.setUsername(RABBITMQ_USER);
      factory.setPassword(RABBITMQ_PASS);
      connection = factory.newConnection();
      channel = connection.createChannel();
      channel.queueDeclare(QUEUE_NAME, true, false, false, null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sendMessage(LiftRide liftRide, int resortID, String seasonID, String dayID, int skierID) {
    try {
      String message = gson.toJson(new LiftRideMessage(liftRide, resortID, seasonID, dayID, skierID));
      channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  static class LiftRideMessage {
    int resortID;
    String seasonID;
    String dayID;
    int skierID;
    int time;
    int liftID;

    public LiftRideMessage(LiftRide liftRide, int resortID, String seasonID, String dayID, int skierID) {
      this.resortID = resortID;
      this.seasonID = seasonID;
      this.dayID = dayID;
      this.skierID = skierID;
      this.time = liftRide.getTime();
      this.liftID = liftRide.getLiftID();
    }
  }
}

