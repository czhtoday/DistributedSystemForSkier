package com.upic.db;

import com.upic.model.LiftRide;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.concurrent.*;

public class DynamoDBWriter {
  private static final String TABLE_NAME = "LiftRides";
  private static final DynamoDbClient dynamoDbClient;

  // Limit batch size to DynamoDB max batch (25) and queue size to prevent OOM
  private static final int BATCH_SIZE = 25;
  private static final int MAX_QUEUE_CAPACITY = 2000;
  private static final BlockingQueue<Map<String, AttributeValue>> bufferQueue = new LinkedBlockingQueue<>(MAX_QUEUE_CAPACITY);

  static {
    dynamoDbClient = DynamoDbClient.builder()
        .region(Region.US_WEST_2)
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();

    Thread batchWriterThread = new Thread(DynamoDBWriter::batchWriter);
    batchWriterThread.setDaemon(true);
    batchWriterThread.start();
  }

  /**
   * Convert lift ride data into a DynamoDB item and enqueue it.
   */
  public static void writeLiftRide(int skierID, int resortID, String seasonID, String dayID, LiftRide liftRide) {
    try {
      // As seasonID and dayID is fixed, add a random number to avoid hot partition
      String dateKey = seasonID + "_" + dayID + "_" + getRandomSuffix();
      int vertical = liftRide.getLiftID() * 10;

      Map<String, AttributeValue> item = new HashMap<>();
      item.put("skierID", AttributeValue.builder().n(String.valueOf(skierID)).build());
      item.put("dateKey", AttributeValue.builder().s(dateKey).build());
      item.put("resortID", AttributeValue.builder().n(String.valueOf(resortID)).build());
      item.put("seasonID", AttributeValue.builder().s(seasonID).build());
      item.put("dayID", AttributeValue.builder().s(dayID).build());
      item.put("liftID", AttributeValue.builder().n(String.valueOf(liftRide.getLiftID())).build());
      item.put("time", AttributeValue.builder().n(String.valueOf(liftRide.getTime())).build());
      item.put("vertical", AttributeValue.builder().n(String.valueOf(vertical)).build());

      // Block if queue is full (backpressure)
      bufferQueue.put(Collections.unmodifiableMap(item));
    } catch (Exception e) {
      System.err.println("[✘] Failed to enqueue item: " + e.getMessage());
    }
  }

  /**
   * Continuously consume buffered items and write to DynamoDB in batches.
   */
  private static void batchWriter() {
    List<Map<String, AttributeValue>> batch = new ArrayList<>();

    while (true) {
      try {
        // Block until first item is available
        Map<String, AttributeValue> first = bufferQueue.take();
        batch.add(first);
        bufferQueue.drainTo(batch, BATCH_SIZE - 1);

        List<WriteRequest> writeRequests = new ArrayList<>();
        for (Map<String, AttributeValue> item : batch) {
          if (item != null && !item.isEmpty()) {
            PutRequest putRequest = PutRequest.builder().item(item).build();
            writeRequests.add(WriteRequest.builder().putRequest(putRequest).build());
          }
        }

        Map<String, List<WriteRequest>> requestItems = new HashMap<>();
        requestItems.put(TABLE_NAME, writeRequests);

        BatchWriteItemRequest batchRequest = BatchWriteItemRequest.builder()
            .requestItems(requestItems)
            .build();

        BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(batchRequest);

        int successCount = batch.size() - response.unprocessedItems().getOrDefault(TABLE_NAME, List.of()).size();
        System.out.println("[✔] Batch wrote " + successCount + " items to DynamoDB");

        // Retry unprocessed items if necessary (simple one retry)
        List<WriteRequest> unprocessed = response.unprocessedItems().get(TABLE_NAME);
        if (unprocessed != null && !unprocessed.isEmpty()) {
          System.out.println("[!] Retrying " + unprocessed.size() + " unprocessed items");
          Map<String, List<WriteRequest>> retryMap = new HashMap<>();
          retryMap.put(TABLE_NAME, unprocessed);
          dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder().requestItems(retryMap).build());
        }

        batch.clear();
      } catch (Exception e) {
        System.err.println("[✘] Batch write failed: " + e.getMessage());
        batch.clear();
      }
    }
  }

  private static String getRandomSuffix() {
    int rand = ThreadLocalRandom.current().nextInt(10); // Generates 0–9
    return "r" + rand;
  }
}