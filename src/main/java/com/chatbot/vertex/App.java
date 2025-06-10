package com.chatbot.vertex;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import io.github.cdimascio.dotenv.Dotenv;


import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {

        Dotenv dotenv = Dotenv.load();
        
        String projectId = dotenv.get("PROJECT_ID");
        String location = dotenv.get("LOCATION");

        String modelName = "gemini-2.0-flash-001";

        // Generation configuration
        GenerationConfig config = GenerationConfig.newBuilder()
                .setMaxOutputTokens(1024)
                .setTemperature(0.7f)
                .setTopP(0.95f)
                .build();

        // Instructions
        String systemInstruction = "You are a helpful assistant that provides concise and accurate answers to user queries.";
        String userInstr = "What is the capital of Saudi Arabia?also tell me what model are you using(specify the exact version)?";

        // Initialize VertexAI and the model in a try-with-resources block
        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
            GenerativeModel model = new GenerativeModel.Builder()
                    .setModelName(modelName)
                    .setVertexAi(vertexAI)
                    .setSystemInstruction(ContentMaker.fromString(systemInstruction))
                    .setGenerationConfig(config)
                    .build();

            // Generate content
            System.out.println("Sending prompt: " + userInstr);
            GenerateContentResponse response = model.generateContent(ContentMaker.fromString(userInstr));

            // Print the response using the helper
            String result = ResponseHandler.getText(response);
            System.out.println("\nChatbot Response:\n" + result);

        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}