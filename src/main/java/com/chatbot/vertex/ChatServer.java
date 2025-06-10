package com.chatbot.vertex;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import java.io.IOException;

public class ChatServer {

    private GenerativeModel model;
    private Javalin app;
    private final int port;

    public ChatServer(int port) {
        this.port = port;
    }

    /**
     * Initializes the Vertex AI model and starts the Javalin server on a background thread.
     */
    public void start() {
        System.out.println("Initializing Vertex AI model...");
        try {
            initializeVertexAI();
            System.out.println("Model initialized successfully.");
        } catch (IOException e) {
            System.err.println("FATAL: Could not initialize Vertex AI model.");
            throw new RuntimeException(e);
        }

        app = Javalin.create()
            .post("/chat", ctx -> {
                String userPrompt = ctx.body();
                // System.out.println("\n[Server] Received prompt: \"" + userPrompt + "\"");
                String botResponse = getChatbotResponse(userPrompt);
                ctx.result(botResponse).contentType("text/plain");
            });

        // Use a daemon thread so the app can exit even if the server is running
        Thread serverThread = new Thread(() -> app.start(port));
        serverThread.setDaemon(true); 
        serverThread.start();
        
        System.out.println("Chat server has been started in the background on port " + port);
    }

    /**
     * Stops the running Javalin server.
     */
    public void stop() {
        if (app != null) {
            app.stop();
            System.out.println("Chat server has been stopped.");
        }
    }

    private String getChatbotResponse(String userPrompt) {
        try {
            GenerateContentResponse response = model.generateContent(ContentMaker.fromString(userPrompt));
            return ResponseHandler.getText(response);
        } catch (Exception e) {
            System.err.println("Error getting response from Vertex AI: " + e.getMessage());
            return "Sorry, I encountered an error while processing your request.";
        }
    }

    private void initializeVertexAI() throws IOException {
        Dotenv dotenv = Dotenv.load();
        String projectId = dotenv.get("PROJECT_ID");
        String location = dotenv.get("LOCATION");
        String modelName = "gemini-2.0-flash-001";
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