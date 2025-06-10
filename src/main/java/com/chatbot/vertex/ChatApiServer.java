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

public class ChatApiServer {

    private static GenerativeModel model;

    public static void main(String[] args) {
        // 1. Initialize the Vertex AI Model (just like before)
        try {
            System.out.println("Initializing Vertex AI model...");
            initializeVertexAI();
            System.out.println("Model initialized successfully.");
        } catch (IOException e) {
            System.err.println("FATAL: Could not initialize Vertex AI model.");
            e.printStackTrace();
            return;
        }

        // 2. Create and start the Javalin web server
        Javalin app = Javalin.create().start(7070); // Server runs on port 7070
        System.out.println("Chat API server started on http://localhost:7070");

        // 3. Define the API endpoint for chatting
        // This handles POST requests to http://localhost:7070/chat
        app.post("/chat", ctx -> {
            String userPrompt = ctx.body(); // Get the prompt from the request body
            System.out.println("Received prompt: \"" + userPrompt + "\"");

            // Get the response from our existing helper method
            String botResponse = getChatbotResponse(userPrompt);

            // Send the response back to the client
            ctx.result(botResponse);
            ctx.contentType("text/plain");
        });
    }

    // This method is identical to the one in the socket example
    private static String getChatbotResponse(String userPrompt) {
        try {
            GenerateContentResponse response = model.generateContent(ContentMaker.fromString(userPrompt));
            return ResponseHandler.getText(response);
        } catch (Exception e) {
            System.err.println("Error getting response from Vertex AI: " + e.getMessage());
            return "Sorry, I encountered an error while processing your request.";
        }
    }

    // This method is also identical
    private static void initializeVertexAI() throws IOException {
        Dotenv dotenv = Dotenv.load();
        String projectId = dotenv.get("PROJECT_ID");
        String location = dotenv.get("LOCATION");
        String modelName = "gemini-2.0-flash-lite-001";

        GenerationConfig config = GenerationConfig.newBuilder()
                .setMaxOutputTokens(1024)
                .setTemperature(0.7f)
                .setTopP(0.95f)
                .build();

        String systemInstruction = "You are a helpful assistant that provides concise and accurate answers to user queries.";

        VertexAI vertexAI = new VertexAI(projectId, location);
        model = new GenerativeModel.Builder()
                .setModelName(modelName)
                .setVertexAi(vertexAI)
                .setSystemInstruction(ContentMaker.fromString(systemInstruction))
                .setGenerationConfig(config)
                .build();
    }
}