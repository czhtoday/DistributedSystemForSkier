# Assignment2: SkierClient - Load Testing for Skier Lift Ride System

## Author
Zhihang Cheng

## Overview
The **SkierClient** is a multi-threaded load testing application designed to evaluate the performance of the **Skier Lift Ride Processing System**. It simulates thousands of skier lift ride events and sends them to the `SkierServer`, measuring latency, throughput, and system behavior under heavy load.

## How It Works
1. **Single Thread Latency Test**: Sends **10,000** sequential requests to measure baseline response time.
2. **Multi-Threaded Load Test**:
   - **Phase 1**: 32 threads send up to **32,000 requests**.
   - **Phase 2**: A dynamic pool of up to **512 threads** processes the remaining **168,000 requests**.
3. **Metrics Collection**:
   - Total number of successful and failed requests.
   - Average latency (single-threaded test).
   - Estimated throughput using **Little’s Law**.
   - Overall system throughput (requests/sec).

## System Components
- **LiftRideEventGenerator**: Generates random skier lift ride events.
- **LiftRideEventQueue**: Stores events before processing.
- **LiftRideEventWorker**: Handles HTTP requests to `SkierServer` asynchronously.
- **API Client**: Uses Swagger-generated client (`SkiersApi`) to interact with `SkierServer`.


