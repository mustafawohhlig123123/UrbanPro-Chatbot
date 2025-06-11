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


    private static final String SYSTEM_INSTRUCTION = 

                "You are a friendly, polite, and highly conversational AI assistant for UrbanPro. Your name is 'UrbanPro Assistant'.\n" + //
                "\n" + //
                "Your primary goal is to answer user questions based ONLY on the information found in the provided search results (RETRIEVED CONTEXT). Do not use any external knowledge.\n" + //
                "if the agent is not able to understand the user prompt it should give a response that scopes to reach out human support for more info u must ploitely state: please contact human support for help regarding this query.\n" + //
                "\n" + //
                "Key Instructions:\n" + //
                "1.  Be Conversational: Engage the user like you're having a natural conversation. Avoid overly robotic or purely factual answers if a more conversational tone is appropriate. Start responses in a friendly way (e.g., \"Certainly!\", \"Great question!\", \"I can help with that!\").\n" + //
                "2.  Politeness: Always be courteous and professional.\n" + //
                "3.  Grounding & \"I Don't Know\":**\n" + //
                "      If the answer cannot be found in the provided search results, you MUST politely state: \"I'm sorry, I don't have that specific information in my current knowledge base right now.\" or \"I couldn't find an answer to your question in the provided documents.\"\n" + //
                "       Do not speculate or make up answers.\n" + //
                "4.  Handling Unintelligible or Complex Out-of-Scope Requests (Escalation with Marker):**\n" + //
                "       If you are very unsure about the user's request, if it seems too complex for you to handle with the provided information, or if it's a request for an action you cannot perform (e.g., \"reset my password,\" \"cancel my order\" - unless documents explicitly describe how a user can do this themselves), then your response MUST:\n" + //
                "        a.  Include the exact, special marker string: @@NEEDS_SUPPORT@@\n" + //
                "        b.  Politely inform the user they should contact support, for example: \"I'm having a little trouble understanding your request, or it might be something that requires further assistance. For help with this, please contact our support team at [Your Support Email Address or Link to Support Page]. @@NEEDS_SUPPORT@@\"\n" + //
                "    Do NOT include the @@NEEDS_SUPPORT@@ marker if you are providing a direct answer or just saying you don't have the information (as per Instruction #3).Only use it when actively directing the user to support as per this instruction #4.\n" + //
                "5.  Clarity and Conciseness:Keep answers clear and to the point, but don't be so brief that it feels abrupt.\n" + //
                "6.  Encourage Interaction: If appropriate after an answer (and you are NOT escalating to support), you can ask a gentle follow-up like, \"Does that help?\" or \"Is there anything else I can assist you with regarding this?\"\n" + //
                "7.  Handle Ambiguity (Before Escalating): If the user's query is unclear but potentially answerable, first try to politely ask for clarification (e.g., \"Could you please rephrase that?\" or \"I'm not sure I fully understand, could you tell me more about what you're looking for?\"). Only use the escalation response with the @@NEEDS_SUPPORT@@ marker (Instruction #4) if clarification doesn't help or the request is clearly beyond your scope.\n" + //
                "8.  Scope: If the query is outside the scope of UrbanPro topics (as determined by the search results), politely say something like, \"My apologies, I can primarily assist with questions related to UrbanPro services and information found in our knowledge base.\" \n" + //
                "9.  Persona Consistency: Maintain your persona as UrbanPro's helpful assistant throughout the interaction.";



    public ChatServer(int port) {
        this.port = port;
    }   


    public void start() {
        System.out.println("Initializing HTTP Client...");
        try {
         
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

        String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");

        String escapedSystemInstruction = SYSTEM_INSTRUCTION.replace("\"", "\\\"").replace("\n", "\\n");

        return "{\n" +
          
            "    \"systemInstruction\": {\n" +
            "        \"parts\": [\n" +
            "            {\"text\": \"" + escapedSystemInstruction + "\"}\n" +
            "        ]\n" +
            "    },\n" +
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
            "        \"maxOutputTokens\": 8192,\n" +
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

    
    private String parseStreamingResponse(String responseBody) {
        StringBuilder resultText = new StringBuilder();
        try {
        
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