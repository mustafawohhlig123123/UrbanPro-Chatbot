package com.chatbot.vertex;

import com.google.auth.oauth2.GoogleCredentials;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;

public class ChatServer {

    private OkHttpClient httpClient;
    private Javalin app;
    private final int port;

    private static final Dotenv dotenv = Dotenv.load();

    private static final String PROJECT_ID = dotenv.get("PROJECT_ID");
    private static final String LOCATION_ID = dotenv.get("LOCATION");
    private static final String API_ENDPOINT = dotenv.get("API_ENDPOINT");
    private static final String MODEL_ID = "gemini-2.0-flash-001";
    private static final String GENERATE_CONTENT_API = "streamGenerateContent";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() {
        System.out.println("Initializing HTTP Client...");
        try {
            // Initialize the HTTP client
            this.httpClient = new OkHttpClient();
            System.out.println("HTTP Client initialized successfully.");
        } catch (Exception e) {
            System.err.println("FATAL: Could not initialize.");
            throw new RuntimeException(e);
        }

        app = Javalin.create()
            .post("/chat", ctx -> {
                String userPrompt = ctx.body();
                System.out.println("\n[Server] Received prompt: \"" + userPrompt + "\"");
                String botResponse = getChatbotResponse(userPrompt);
                ctx.result(botResponse).contentType("text/plain");
            });


        Thread serverThread = new Thread(() -> app.start(port));
        serverThread.setDaemon(true);
        serverThread.start();

        System.out.println("Chat server has been started in the background on port " + port);
    }

    public void stop() {
        if (app != null) {
            app.stop();
            System.out.println("Chat server has been stopped.");
        }
    }

    /**
     * Replicates the provided curl command to make a direct HTTP request to the Vertex AI API.
     */
    private String getChatbotResponse(String userPrompt) {
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
            credentials.refreshIfExpired();
            String accessToken = credentials.getAccessToken().getTokenValue();

            String apiUrl = String.format("https://%s/v1/projects/%s/locations/%s/publishers/google/models/%s:%s",
                API_ENDPOINT, PROJECT_ID, LOCATION_ID, MODEL_ID, GENERATE_CONTENT_API);

  
            String jsonBody = buildJsonRequestBody(userPrompt);

            RequestBody body = RequestBody.create(jsonBody, JSON);
            Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (!response.isSuccessful()) {
                    System.err.printf("API Error: %d %s - %s%n", response.code(), response.message(), responseBody);
                    return "Sorry, I encountered an API error. Status: " + response.code();
                }
                

                return parseStreamingResponse(responseBody);
            }
        } catch (IOException e) {
            System.err.println("Error during HTTP request to Vertex AI: " + e.getMessage());
            e.printStackTrace();
            return "Sorry, a network error occurred while processing your request.";
        }
    }


    private String buildJsonRequestBody(String prompt) {
        // Basic escaping for the prompt to be safely inserted into the JSON string
        String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");

        // The user prompt is inserted into the `parts` array.
        // NOTE: The API uses "BLOCK_NONE" instead of "OFF" for safety settings.
        return "{\n" +
            "    \"contents\": [\n" +
            "        {\n" +
            "            \"role\": \"user\",\n" +
            "            \"parts\": [\n" +
            "                {\"text\": \"" + escapedPrompt + "\"}\n" +
            "            ]\n" +
            "        }\n" +
            "    ],\n" +
            "    \"generationConfig\": {\n" +
            "        \"temperature\": 1,\n" +
            "        \"maxOutputTokens\": 8192,\n" + //65535 is often too highusing a more standard 8192
            "        \"topP\": 1,\n" +
            "        \"seed\": 0\n" +
            "    },\n" +
            "    \"safetySettings\": [\n" +
            "        {\"category\": \"HARM_CATEGORY_HATE_SPEECH\", \"threshold\": \"BLOCK_NONE\"},\n" +
            "        {\"category\": \"HARM_CATEGORY_DANGEROUS_CONTENT\", \"threshold\": \"BLOCK_NONE\"},\n" +
            "        {\"category\": \"HARM_CATEGORY_SEXUALLY_EXPLICIT\", \"threshold\": \"BLOCK_NONE\"},\n" +
            "        {\"category\": \"HARM_CATEGORY_HARASSMENT\", \"threshold\": \"BLOCK_NONE\"}\n" +
            "    ],\n" +
            "    \"tools\": [\n" +
            "        {\n" +
            "            \"retrieval\": {\n" +
            "                \"vertexRagStore\": {\n" +
            "                    \"ragResources\": [\n" +
            "                        {\"ragCorpus\": \"projects/proj-newsshield-prod-infra/locations/us-central1/ragCorpora/6917529027641081856\"}\n" +
            "                    ],\n" +
            "                    \"ragRetrievalConfig\": {\n" +
            "                        \"topK\": 20\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";
    }

    /**
     * Parses the streaming JSON response from the Gemini API and extracts all text content.
     */
    private String parseStreamingResponse(String responseBody) {
        StringBuilder resultText = new StringBuilder();
        try {
            // The streaming response is a JSON array of response objects.
            JSONArray responses = new JSONArray(responseBody);
            
            for (int i = 0; i < responses.length(); i++) {
                JSONObject responseObj = responses.getJSONObject(i);
                if (responseObj.has("candidates")) {
                    JSONArray candidates = responseObj.getJSONArray("candidates");
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    JSONObject firstPart = parts.getJSONObject(0);
                    if (firstPart.has("text")) {
                         resultText.append(firstPart.getString("text"));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse streaming response: " + e.getMessage());
            System.err.println("Response Body was: " + responseBody);
            return "Error: Could not parse the AI's response.";
        }
        return resultText.toString();
    }
}