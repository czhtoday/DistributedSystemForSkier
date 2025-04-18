package com.upic.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQConfig {
//  private static final String RABBITMQ_HOST = "localhost";
  private static final String RABBITMQ_HOST = "54.189.112.229"; // RabbitMQ server IP
  private static final String RABBITMQ_USER = "guest";
  private static final String RABBITMQ_PASS = "guest";

  private static Connection connection;

  static {
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(RABBITMQ_HOST);
      factory.setUsername(RABBITMQ_USER);
      factory.setPassword(RABBITMQ_PASS);
      connection = factory.newConnection();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to connect to RabbitMQ");
    }
  }

  public static Connection getConnection() {
    return connection;
  }
}
