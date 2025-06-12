package com.chatbot.vertex;

import okhttp3.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class ChatClient {

    private final String serverUrl;
    private final OkHttpClient httpClient;
    
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
   
    private static final String USER_ID = "user-002";

    public ChatClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = new OkHttpClient();
    }

    public void runConsoleSession() {
        
        System.out.println("Chat client started for user: " + USER_ID);
        System.out.println("Type 'exit' to quit.");

        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            String userInput;
            while (true) {
                System.out.print("You: ");
                userInput = consoleReader.readLine();

                if (userInput == null || "exit".equalsIgnoreCase(userInput.trim())) {
                    break;
                }
                
                if (userInput.trim().isEmpty()) {
                    continue; 
                }

                // 1. Create the JSON payload string using the hardcoded USER_ID.
                String jsonPayload = String.format("{\"userId\": \"%s\", \"prompt\": \"%s\"}",
                                                   USER_ID,
                                                   escapeJson(userInput)); 

                // 2. Create a request body with the JSON payload.
                RequestBody body = RequestBody.create(jsonPayload, JSON);

                // 3. Build the POST request.
                Request request = new Request.Builder()
                        .url(serverUrl)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        System.err.println("Server error: " + response.code());
                        if (response.body() != null) {
                            System.err.println("Error details: " + response.body().string());
                        }
                    } else {
                        String botResponse = response.body() != null ? response.body().string() : "[No response]";
                        System.out.println("Bot: " + botResponse);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("An error occurred. Is the server running? Details: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Helper function to ensure the user's input doesn't break the JSON format.
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}