package com.upic.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.upic.model.LiftRide;
import com.upic.queue.MessageQueueProducer;


import java.io.BufferedReader;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

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
