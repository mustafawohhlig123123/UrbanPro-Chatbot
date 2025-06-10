package com.chatbot.vertex;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChatApiClient {

    private static final String SERVER_URL = "http://localhost:7070/chat";

    public static void main(String[] args) {
        // Create an HttpClient instance
        HttpClient client = HttpClient.newHttpClient();
        
        System.out.println("Connected to ChatBot API. Type 'exit' to quit.");
        System.out.println("==============================================");

        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            String userInput;
            while (true) {
                System.out.print("You: ");
                userInput = consoleReader.readLine();

                if (userInput == null || "exit".equalsIgnoreCase(userInput.trim())) {
                    break;
                }

                // Create an HTTP POST request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SERVER_URL))
                        .header("Content-Type", "text/plain")
                        .POST(HttpRequest.BodyPublishers.ofString(userInput))
                        .build();

                // Send the request and get the response
                // The response body is handled as a String
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Print the chatbot's response
                System.out.println("Chatbot: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("An error occurred. Is the ChatApiServer running?");
            e.printStackTrace();
        }
        System.out.println("Disconnected.");
    }
}