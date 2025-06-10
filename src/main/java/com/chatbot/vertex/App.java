// File: src/main/java/com/chatbot/vertex/App.java
package com.chatbot.vertex;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class App {

    private static GenerativeModel model;
    private static final int PORT = 7070;
    private static final String SERVER_URL = "http://localhost:" + PORT + "/chat";

    public static void main(String[] args) throws InterruptedException {
        // --- 1. Start the API Server in a Background Thread ---
        System.out.println("Starting background chat server...");
        Javalin app = startServer();

        // Give the server a moment to start up before the client tries to connect.
        Thread.sleep(2000); // 2 seconds

        // --- 2. Run the Client Console in the Main Thread ---
        System.out.println("Server started. You can now begin chatting.");
        startClientConsole();

        // --- 3. Stop the server when the client console exits ---
        System.out.println("Shutting down server...");
        app.stop();
        System.out.println("Application finished.");
    }

    /**
     * Initializes and starts the Javalin web server.
     * @return The Javalin app instance, which can be used to stop the server.
     */
    private static Javalin startServer() {
        try {
            initializeVertexAI();
        } catch (IOException e) {
            System.err.println("FATAL: Could not initialize Vertex AI model.");
            throw new RuntimeException(e);
        }

        Javalin app = Javalin.create().get("/", ctx -> ctx.result("Chat server is running."));
        // Define the API endpoint for chatting
        app.post("/chat", ctx -> {
            String userPrompt = ctx.body();
            // Log server-side activity
            System.out.println("\n[Server] Received prompt: \"" + userPrompt + "\"");
            String botResponse = getChatbotResponse(userPrompt);
            ctx.result(botResponse);
            ctx.contentType("text/plain");
        });

        // Start the server on a new daemon thread.
        // A daemon thread will not prevent the application from exiting.
        Thread serverThread = new Thread( () -> app.start(PORT) );
        serverThread.setDaemon(true);
        serverThread.start();

        return app;
    }

    /**
     * Starts the interactive command-line client.
     */
    private static void startClientConsole() {
        HttpClient client = HttpClient.newHttpClient();
        System.out.println("==============================================");
        System.out.println("Connected to ChatBot. Type 'exit' to quit.");
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
                        .uri(URI.create(SERVER_URL))
                        .POST(HttpRequest.BodyPublishers.ofString(userInput))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Chatbot: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("An error occurred in the client: " + e.getMessage());
        }
    }


    // --- Helper methods for Vertex AI (unchanged) ---

    private static String getChatbotResponse(String userPrompt) {
        try {
            GenerateContentResponse response = model.generateContent(ContentMaker.fromString(userPrompt));
            return ResponseHandler.getText(response);
        } catch (Exception e) {
            return "Sorry, I encountered an error.";
        }
    }

    private static void initializeVertexAI() throws IOException {
        Dotenv dotenv = Dotenv.load();
        String projectId = dotenv.get("PROJECT_ID");
        String location = dotenv.get("LOCATION");
        String modelName = "gemini-2.0-flash-lite-001";
        GenerationConfig config = GenerationConfig.newBuilder().setMaxOutputTokens(1024).build();
        VertexAI vertexAI = new VertexAI(projectId, location);
        model = new GenerativeModel.Builder()
                .setModelName(modelName)
                .setVertexAi(vertexAI)
                .setSystemInstruction(ContentMaker.fromString("You are a helpful assistant."))
                .setGenerationConfig(config)
                .build();
    }
}