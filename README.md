# CS6650Assignment3 - Skier Lift Ride Data Persistence System

## Author
Zhihang Cheng

## Overview
This project extends the scalable distributed system from Assignment 2 by **persisting skier lift ride events into a database**. It continues using a microservices architecture with asynchronous message queue processing and adds a high-throughput data storage layer.

### Main Components:

1. **skierClient**: A multi-threaded client that generates and sends 200,000 simulated skier lift ride events for performance testing.
2. **skierServer**: A servlet-based backend that validates requests and forwards events to a RabbitMQ queue.
3. **consumerService**: A multi-threaded consumer that reads events from RabbitMQ and writes them to **DynamoDB** in **batched mode** for improved throughput.

## System Workflow
1. `skierClient` sends POST requests to `skierServer`, each representing a skier lift ride event.
2. `skierServer`:
   - Validates all parameters (`skierID`, `resortID`, `seasonID`, `dayID`, `liftID`, `time`)
   - Sends valid messages to RabbitMQ (`lift_ride_queue`)
3. `consumerService`:
   - Consumes messages concurrently using 8–16 threads per EC2 instance
   - Buffers events in memory and writes them to DynamoDB in batches of 25
   - Supports horizontal scaling by adding more consumer EC2 instances

## Key Achievements
-  **High Client Throughput**: Achieved up to **8,115 requests/sec** on the client with 0 failed requests.
-  **100% Data Integrity**: All 200,000 records successfully validated and persisted without loss.
-  **Asynchronous, Decoupled Pipeline**: RabbitMQ buffers messages between servlet and database layer, smoothing spikes in load.
-  **Scalable Consumers**: Multiple consumer EC2 instances enabled parallel batch writes to DynamoDB.
-  **DynamoDB Optimizations**:
  - Batched writes (up to 25 items per call)
  - Retry logic for unprocessed items
  - Partition key design: `skierID` + `dateKey` (`seasonID_dayID`)

## Technologies Used
- Java, Maven, Tomcat
- RabbitMQ
- AWS EC2
- AWS DynamoDB
- Gson, AWS SDK v2

## Improvements Over Assignment 2
- Added persistent data storage via DynamoDB.
- Implemented batch writing and thread pooling to boost consumer efficiency.
- Improved system observability and throughput reporting.

