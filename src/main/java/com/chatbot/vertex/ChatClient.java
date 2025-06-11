package com.chatbot.vertex;

import okhttp3.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ChatClient {

    private final String serverUrl;
    private final OkHttpClient httpClient;
    private final MediaType TEXT_PLAIN = MediaType.parse("text/plain; charset=utf-8");

    public ChatClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = new OkHttpClient();
    }

    public void runConsoleSession() {
        System.out.println("Chat client started. Type 'exit' to quit.");

        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            String userInput;
            while (true) {
                System.out.print("You: ");
                userInput = consoleReader.readLine();

                if (userInput == null || "exit".equalsIgnoreCase(userInput.trim())) {
                    break;
                }

                RequestBody body = RequestBody.create(userInput, TEXT_PLAIN);
                Request request = new Request.Builder()
                        .url(serverUrl)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        System.err.println("Server error: " + response.code());
                    } else {
                        System.out.println("Chatbot: " + response.body().string());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("An error occurred. Is the server running? Details: " + e.getMessage());
        }
    }
}
