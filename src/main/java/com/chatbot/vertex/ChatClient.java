package com.chatbot.vertex;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChatClient {

    private final String serverUrl;
    private final HttpClient httpClient;

    public ChatClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Starts the interactive command-line session to chat with the server.
     */
    public void runConsoleSession() {
        System.out.println("==============================================");
        System.out.println("Chat client started. Type 'exit' to quit.");
        System.out.println("==============================================");

        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            String userInput;
            while (true) {
                System.out.print("You: ");
                userInput = consoleReader.readLine();

                if (userInput == null || "exit".equalsIgnoreCase(userInput.trim())) {
                    break;
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl))
                        .POST(HttpRequest.BodyPublishers.ofString(userInput))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Chatbot: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("An error occurred. Is the server running? Details: " + e.getMessage());
        }
    }
}