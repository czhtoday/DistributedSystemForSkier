package com.upic.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.upic.model.LiftRide;
import com.upic.queue.MessageQueueProducer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import java.net.URI;

/**
 * Servlet for handling skier lift ride events.
 * This servlet processes POST requests to record lift ride data for a specific skier.
 *
 * Expected URL format:
 * POST /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
 *
 * Request body (JSON format):
 * {
 *   "time": <int>,  // Time of the lift ride (1-360)
 *   "liftID": <int>  // Lift ID (1-40)
 * }
 *
 * Validates both path parameters and request body before recording the event.
 */
public class SkierServlet extends HttpServlet {
    private final Gson gson = new Gson(); // JSON parser for request body deserialization
    private Properties validationProps;

    /**
     * Init and load resources file as constraints
     * @throws ServletException
     */
    @Override
    public void init() throws ServletException {
        super.init();

        // load validation.properties file
        try {
            validationProps = new Properties();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("validation.properties");

            if (inputStream != null) {
                validationProps.load(inputStream);
                inputStream.close();
            } else {
                throw new ServletException("Could not find validation.properties");
            }
        } catch (IOException e) {
            throw new ServletException("Error loading validation properties", e);
        }
    }

    /**
     * Handles POST requests to record a skier's lift ride event.
     *
     * @param request  The HTTP request containing path parameters and a JSON body.
     * @param response The HTTP response indicating success or failure.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException      If an input or output error occurs while handling the request.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Set response content type to JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // Extract path parameters from the URL
            String pathInfo = request.getPathInfo(); // Retrieves the part after /skiers/
            if (pathInfo == null || pathInfo.split("/").length != 8) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format");
                return;
            }

            // Parse path parameters
            String[] pathParts = pathInfo.split("/");
            int resortID, skierID;
            String seasonID, dayID;

            try {
                resortID = Integer.parseInt(pathParts[1]);  // /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
                seasonID = pathParts[3];
                dayID = pathParts[5];
                skierID = Integer.parseInt(pathParts[7]);
            } catch (NumberFormatException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid number format in URL");
                return;
            }

            // Validate path parameters
            if (resortID < 1 || resortID > 10 || !seasonID.equals("2025") || !dayID.equals("1") || skierID < 1 || skierID > 100000) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid path parameters");
                return;
            }

            // Read JSON request body
            StringBuilder jsonBuilder = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            LiftRide liftRide;
            try {
                liftRide = gson.fromJson(jsonBuilder.toString(), LiftRide.class);
            } catch (JsonSyntaxException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format");
                return;
            }


            // Validate request body parameters
            if (liftRide == null || liftRide.getTime() < 1 || liftRide.getTime() > 360 || liftRide.getLiftID() < 1 || liftRide.getLiftID() > 40) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON payload");
                return;
            }

            // Send validated data to RabbitMQ
            try {
                MessageQueueProducer.sendMessage(liftRide, resortID, seasonID, dayID, skierID);
                sendSuccessResponse(response, HttpServletResponse.SC_CREATED, "Lift ride added to queue");
            } catch (Exception e) {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to enqueue message");
            }

        } catch (Exception e) {
            // Catch any unexpected errors and return a generic server error response
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        String servletPath = request.getServletPath();

        if (pathInfo == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format");
            return;
        }

        String[] parts = pathInfo.split("/");

        // Handle GET /skiers/{skierID}/vertical
        if (parts.length == 3 && parts[1].matches("\\d+") && parts[2].equals("vertical")) {
            try {
                int skierID = Integer.parseInt(parts[1]);
                handleGetVertical(skierID, response);
            } catch (NumberFormatException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid skierID");
            }
            return;
        }

        // Handle GET/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
        if (parts.length == 8 && parts[2].equals("seasons") && parts[4].equals("days") && parts[6].equals("skiers")) {
            try {
                int resortID = Integer.parseInt(parts[1]);
                String seasonID = parts[3];
                String dayID = parts[5];
                int skierID = Integer.parseInt(parts[7]);

                handleGetSkierDayData(resortID, seasonID, dayID, skierID, response);
                return;
            } catch (NumberFormatException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid number format in URL");
            }
            return;
        }

        // Handle /resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
        if ("/resorts".equals(servletPath) && parts.length >= 7 &&
                "seasons".equals(parts[2]) && "day".equals(parts[4]) &&
                "skiers".equals(parts[6])) {
            try {
                int resortID = Integer.parseInt(parts[1]);
                String seasonID = parts[3];
                String dayID = parts[5];

                handleGetSkiersByDay(resortID, seasonID, dayID, response);
                return;
            } catch (NumberFormatException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid resortID");
                return;
            } catch (Exception e) {
                e.printStackTrace();
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Error processing request: " + e.getMessage());
                return;
            }
        }

        sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Unknown GET path: " + pathInfo);
    }


    private void handleGetVertical(int skierID, HttpServletResponse response) throws IOException {
        try {
            DynamoDbClient dynamoDbClient = DynamoDbClient.create();

            String prefix = "2025_"; // fixed season
            QueryRequest request = QueryRequest.builder()
                    .tableName("LiftRides")
                    .keyConditionExpression("skierID = :skierID AND begins_with(dateKey, :prefix)")
                    .expressionAttributeValues(Map.of(
                            ":skierID", AttributeValue.builder().n(String.valueOf(skierID)).build(),
                            ":prefix", AttributeValue.builder().s(prefix).build()
                    ))
                    .build();

            QueryResponse result = dynamoDbClient.query(request);

            int totalVertical = result.items().stream()
                    .mapToInt(item -> Integer.parseInt(item.get("vertical").n()))
                    .sum();

            String json = gson.toJson(Map.of("skierID", skierID, "totalVertical", totalVertical));
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write(json);

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to get vertical");
        }
    }

    /**
     * Handler for GET API: GET/skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
     * @param resortID
     * @param seasonID
     * @param dayID
     * @param skierID
     * @param response
     * @throws IOException
     */
    private void handleGetSkierDayData(int resortID, String seasonID, String dayID, int skierID,
                                       HttpServletResponse response) throws IOException {
        // get validation boundary from Properties
        int resortMin = Integer.parseInt(validationProps.getProperty("validation.resort.min", "1"));
        int resortMax = Integer.parseInt(validationProps.getProperty("validation.resort.max", "10"));
        String seasonMin = validationProps.getProperty("validation.season.min", "2025");
        String seasonMax = validationProps.getProperty("validation.season.max", "2025");
        int dayMin = Integer.parseInt(validationProps.getProperty("validation.day.min", "1"));
        int dayMax = Integer.parseInt(validationProps.getProperty("validation.day.max", "366"));
        int skierMin = Integer.parseInt(validationProps.getProperty("validation.skier.min", "1"));
        int skierMax = Integer.parseInt(validationProps.getProperty("validation.skier.max", "100000"));

        // validate the values in URL
        if (resortID < resortMin || resortID > resortMax ||
                !seasonID.equals(seasonMin) || // Assume seasonID is equal to seasonMin currently
                Integer.parseInt(dayID) < dayMin || Integer.parseInt(dayID) > dayMax ||
                skierID < skierMin || skierID > skierMax
        ) {

            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid path parameters");
            return;
        }

        try {
            DynamoDbClient dynamoDbClient = DynamoDbClient.create();

            // query items by seasonID, dayID and skierID , which are combined as sortKey in GSI
            String seasonDaySkier = seasonID + "_" + dayID + "_" + skierID;

            QueryRequest request = QueryRequest.builder()
                    .tableName("LiftRides")
                    .indexName("resort-season-day-skier-index")
                    .keyConditionExpression("resortID = :resortID AND seasonDaySkier = :sdsk")
                    .expressionAttributeValues(Map.of(
                            ":resortID", AttributeValue.builder().n(String.valueOf(resortID)).build(),
                            ":sdsk", AttributeValue.builder().s(seasonDaySkier).build()
                    ))
                    .build();

            QueryResponse result = dynamoDbClient.query(request);

            // if not find the item
            if (result.items().isEmpty()) {
                sendSuccessResponse(response, HttpServletResponse.SC_OK,
                        "No records found for skier " + skierID + " on day " + dayID);
                return;
            }

            // Process query result，build JSON
            List<Map<String, Object>> liftRides = new ArrayList<>();

            for (Map<String, AttributeValue> item : result.items()) {
                Map<String, Object> liftRide = new HashMap<>();
                liftRide.put("time", Integer.parseInt(item.get("time").n()));
                liftRide.put("liftID", Integer.parseInt(item.get("liftID").n()));
                liftRide.put("vertical", Integer.parseInt(item.get("vertical").n()));
                liftRides.add(liftRide);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("skierID", skierID);
            responseData.put("resortID", resortID);
            responseData.put("seasonID", seasonID);
            responseData.put("dayID", dayID);
            responseData.put("liftRides", liftRides);

            String json = gson.toJson(responseData);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write(json);

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to get skier day data: " + e.getMessage());
        }
    }

    /**
     * Handle GET /resorts/{resortID}/seasons/{seasonID}/day/{dayID}/skiers
     * Returns a list of skiers at the specified resort on the specified day
     */
    private void handleGetSkiersByDay(int resortID, String seasonID, String dayID, HttpServletResponse response) throws IOException {
        try {
            DynamoDbClient dynamoDbClient = DynamoDbClient.create();

            // Create the dateKey prefix for queries (matches how data is stored in DynamoDB)
            String dateKeyPrefix = seasonID + "_" + dayID + "_";

            // Query DynamoDB for all skiers on this day at this resort using the GSI
            QueryRequest request = QueryRequest.builder()
                    .tableName("LiftRides")
                    .indexName("resortDateIndex") // Use the GSI here
                    .keyConditionExpression("resortID = :resortID AND begins_with(dateKey, :dateKeyPrefix)")
                    .expressionAttributeValues(Map.of(
                            ":resortID", AttributeValue.builder().n(String.valueOf(resortID)).build(),
                            ":dateKeyPrefix", AttributeValue.builder().s(dateKeyPrefix).build()
                    ))
                    .build();

            QueryResponse result = dynamoDbClient.query(request);

            // Extract unique skier IDs and their lift ride data
            Map<Integer, List<Map<String, Object>>> skierRidesMap = new HashMap<>();


            for (Map<String, AttributeValue> item : result.items()) {
                // Check if required properties exist
                if (item.get("skierID") == null || item.get("liftID") == null || item.get("time") == null) {
                    // Skip record if missing required properties
                    continue;
                }
                // For vertical, if missing, set a default value (i.e. 0)
                int skierID = Integer.parseInt(item.get("skierID").n());
                int liftID = Integer.parseInt(item.get("liftID").n());
                int time = Integer.parseInt(item.get("time").n());
                int vertical = (item.get("vertical") != null && item.get("vertical").n() != null)
                    ? Integer.parseInt(item.get("vertical").n())
                    : 0;  // Default to zero if vertical is missing

                Map<String, Object> rideData = new HashMap<>();
                rideData.put("liftID", liftID);
                rideData.put("time", time);
                rideData.put("vertical", vertical);

                // Add to map, creating list if needed
                skierRidesMap.computeIfAbsent(skierID, k -> new ArrayList<>()).add(rideData);
            }


            // Convert to response format
            List<Map<String, Object>> skiersList = new ArrayList<>();
            for (Map.Entry<Integer, List<Map<String, Object>>> entry : skierRidesMap.entrySet()) {
                Map<String, Object> skierData = new HashMap<>();
                skierData.put("skierID", entry.getKey());
                skierData.put("liftRides", entry.getValue());
                skiersList.add(skierData);
            }

            // Create and send the response
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("resortID", resortID);
            responseMap.put("seasonID", seasonID);
            responseMap.put("dayID", dayID);
            responseMap.put("skiers", skiersList);

            String json = gson.toJson(responseMap);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write(json);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = (e.getMessage() != null) ? e.getMessage() : e.toString();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Failed to get skiers for day: " + errorMessage);
        }

    }

    /**
     * Utility method to send a JSON error response.
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write("{\"message\":\"" + message + "\"}");
        }
    }

    /**
     * Utility method to send a JSON success response.
     */
    private void sendSuccessResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write("{\"message\":\"" + message + "\"}");
        }
    }
}