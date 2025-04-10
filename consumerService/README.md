# Assignment3: ConsumerService - Scalable Multi-Threaded Consumer for Lift Ride Events

## Author
Zhihang Cheng

## Overview
The **ConsumerService** is a scalable, multi-threaded RabbitMQ consumer designed for high-throughput data ingestion into **DynamoDB**. It supports concurrent message processing and batched database writes to persist skier lift ride events efficiently.

## Key Features

###  Message Queue Consumption
- Listens to the `lift_ride_queue` from **RabbitMQ**.
- Uses **manual acknowledgment** (ACK/NACK) to ensure reliable message processing.
- Configurable **multi-threaded workers** (default: 8–16 threads) for parallel consumption.

###  Batched DynamoDB Writes
- Instead of writing each record immediately, events are added to an in-memory buffer.
- Records are written in **batches of up to 25 items**, the maximum supported by DynamoDB.
- Automatic retry for unprocessed items (due to throttling or write conflicts).

###  Multi-Threaded Design
- Each thread establishes its own RabbitMQ channel.
- Workers process and enqueue messages concurrently without blocking.
- Thread-safe producer-consumer pattern using `BlockingQueue`.

###  Intelligent Backpressure Handling
- Each consumer uses `channel.basicQos()` to control unacked message flow.
- Minimal logging to reduce I/O overhead during high-throughput scenarios.


## System Components
- **MultiThreadedMessageQueueConsumer**: Launches a pool of RabbitMQ consumer threads.
- **DynamoDBWriter**: Buffers incoming events and flushes to DynamoDB in batches.
- **LiftRideMessage**: POJO representing the message schema from RabbitMQ.
- **LiftRide**: Model class for skier event data (liftID, time, etc).

## How to Run
1. Set up RabbitMQ and ensure the queue `lift_ride_queue` is declared.
2. Configure AWS credentials using the default provider chain (EC2 IAM Role or local config).
3. Build the project with Maven and run: java -jar consumerService-1.0-SNAPSHOT.jar
