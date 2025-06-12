package com.chatbot.vertex;

import com.chatbot.vertex.model.ChatHistory;
import com.chatbot.vertex.model.ChatRequest;
import com.chatbot.vertex.model.Content;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
// <-- END OF ADDED IMPORTS -->

import com.google.auth.oauth2.GoogleCredentials;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Collections;
import java.util.List; // <-- ADDED for List<Content>

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class ChatServer {

    private OkHttpClient httpClient;
    private Javalin app;
    private final int port;

 
    private MongoClient mongoClient;
    private MongoCollection<ChatHistory> historyCollection;

    private static final Dotenv dotenv = Dotenv.load();
    private static final String MONGODB_URI = dotenv.get("MONGODB_URI");
    private static final String MONGODB_DATABASE = dotenv.get("MONGODB_DATABASE");

    private static final String PROJECT_ID = dotenv.get("PROJECT_ID");
    private static final String LOCATION_ID = dotenv.get("LOCATION");
    private static final String API_ENDPOINT = dotenv.get("API_ENDPOINT");
    private static final String MODEL_ID = "gemini-2.0-flash-001";

    private static final String GENERATE_CONTENT_API = "streamGenerateContent";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final String SYSTEM_INSTRUCTION = 

                "You are a friendly, polite, and highly conversational AI assistant for UrbanPro. Your name is 'UrbanPro Assistant'.\n" + 
                "\n" + 
                "Your primary goal is to answer user questions based ONLY on the information found in the provided search results (RETRIEVED CONTEXT). Do not use any external knowledge.\n" + 
                "if the agent is not able to understand the user prompt it should give a response that scopes to reach out human support for more info u must ploitely state: please contact human support for help regarding this query.\n" + 
                "\n" + 
                "Key Instructions:\n" + 
                "1.  Be Conversational: Engage the user like you're having a natural conversation. Avoid overly robotic or purely factual answers if a more conversational tone is appropriate. Start responses in a friendly way (e.g., \"Certainly!\", \"Great question!\", \"I can help with that!\").\n" + 
                "2.  Politeness: Always be courteous and professional.\n" + 
                "3.  Grounding & \"I Don't Know\":**\n" + 
                "      If the answer cannot be found in the provided search results, you MUST politely state: \"I'm sorry, I don't have that specific information in my current knowledge base right now.\" or \"I couldn't find an answer to your question in the provided documents.\"\n" + 
                "       Do not speculate or make up answers.\n" + 
                "4.  Handling Unintelligible or Complex Out-of-Scope Requests (Escalation with Marker):**\n" + 
                "       If you are very unsure about the user's request, if it seems too complex for you to handle with the provided information, or if it's a request for an action you cannot perform (e.g., \"reset my password,\" \"cancel my order\" - unless documents explicitly describe how a user can do this themselves), then your response MUST:\n" + 
                "        a.  Include the exact, special marker string: @@NEEDS_SUPPORT@@\n" + 
                "        b.  Politely inform the user they should contact support, for example: \"I'm having a little trouble understanding your request, or it might be something that requires further assistance. For help with this, please contact our support team at [Your Support Email Address or Link to Support Page]. @@NEEDS_SUPPORT@@\"\n" + 
                "    Do NOT include the @@NEEDS_SUPPORT@@ marker if you are providing a direct answer or just saying you don't have the information (as per Instruction #3).Only use it when actively directing the user to support as per this instruction #4.\n" + 
                "5.  Clarity and Conciseness:Keep answers clear and to the point, but don't be so brief that it feels abrupt.\n" + 
                "6.  Encourage Interaction: If appropriate after an answer (and you are NOT escalating to support), you can ask a gentle follow-up like, \"Does that help?\" or \"Is there anything else I can assist you with regarding this?\"\n" + 
                "7.  Handle Ambiguity (Before Escalating): If the user's query is unclear but potentially answerable, first try to politely ask for clarification (e.g., \"Could you please rephrase that?\" or \"I'm not sure I fully understand, could you tell me more about what you're looking for?\"). Only use the escalation response with the @@NEEDS_SUPPORT@@ marker (Instruction #4) if clarification doesn't help or the request is clearly beyond your scope.\n" + 
                "8.  Scope: If the query is outside the scope of UrbanPro topics (as determined by the search results), politely say something like, \"My apologies, I can primarily assist with questions related to UrbanPro services and information found in our knowledge base.\" \n" + 
                "9.  Persona Consistency: Maintain your persona as UrbanPro's helpful assistant throughout the interaction.";



    public ChatServer(int port) {
        this.port = port;
    }   


    public void start() {
        System.out.println("Initializing services");
        try {
            this.httpClient = new OkHttpClient();

            CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
            CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
            
            MongoClientSettings clientSettings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(MONGODB_URI))
                    .codecRegistry(codecRegistry)
                    .build();

            this.mongoClient = MongoClients.create(clientSettings);
            MongoDatabase database = this.mongoClient.getDatabase(MONGODB_DATABASE);

            this.historyCollection = database.getCollection("chat_histories", ChatHistory.class);
            System.out.println("Successfully connected to MongoDB.");
          

            System.out.println("Services initialized successfully.");
        } 
        
        catch (Exception e) {

            System.err.println("FATAL: Could not initialize services.");
            e.printStackTrace();
            throw new RuntimeException(e);

        }


        app = Javalin.create(config -> {
            config.jsonMapper(new io.javalin.json.JavalinJackson());
        }).post("/chat", ctx -> {
            try {
                ChatRequest chatRequest = ctx.bodyAsClass(ChatRequest.class);
                String userId = chatRequest.getUserId();
                String userPrompt = chatRequest.getPrompt();

            if (userId == null || userId.trim().isEmpty() || userPrompt == null || userPrompt.trim().isEmpty()) {
                ctx.status(400).result("Request must include 'userId' and 'prompt'.");
                return;
            }

                System.out.println("\n[Server] Received prompt from userId '" + userId + "': \"" + userPrompt + "\"");

                // 1. Get history
                ChatHistory chatHistory = this.getChatHistory(userId);
                // 2. Add new user message to history
                chatHistory.addMessage("user", userPrompt);
                // 3. Get response from AI using the full history
                String botResponse = getChatbotResponse(chatHistory.getHistory());
                // 4. Add AI response to history
                chatHistory.addMessage("model", botResponse);
                // 5. Save the complete, updated history back to DB
                this.saveChatHistory(chatHistory);

                ctx.result(botResponse).contentType("text/plain");

            } 
            catch (Exception e) {

                System.err.println("Error processing chat request: " + e.getMessage());
                e.printStackTrace();
                ctx.status(500).result("An internal server error occurred.");

            }
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
        

        if (this.mongoClient != null) { 
            this.mongoClient.close();
            System.out.println("MongoDB connection closed.");
        }
    }


    private String getChatbotResponse(List<Content> history) throws IOException {
        
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refreshIfExpired();

        String accessToken = credentials.getAccessToken().getTokenValue();

        String apiUrl = String.format("https://%s/v1/projects/%s/locations/%s/publishers/google/models/%s:%s",
                API_ENDPOINT, PROJECT_ID, LOCATION_ID, MODEL_ID, GENERATE_CONTENT_API);

  
        String jsonBody = buildJsonRequestBody(history);

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
    }


    private String buildJsonRequestBody(List<Content> history) {
        JSONObject root = new JSONObject();

        JSONObject systemInstruction = new JSONObject();
        systemInstruction.put("parts", new JSONArray().put(new JSONObject().put("text", SYSTEM_INSTRUCTION)));
        root.put("systemInstruction", systemInstruction);

        JSONArray contents = new JSONArray();
        for (Content msg : history) {

            JSONObject message = new JSONObject();
            message.put("role", msg.getRole());
            JSONArray parts = new JSONArray();
            msg.getParts().forEach(part -> parts.put(new JSONObject().put("text", part.getText())));
            message.put("parts", parts);
            contents.put(message);

        }
        root.put("contents", contents);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 1);
        generationConfig.put("maxOutputTokens", 8192);
        generationConfig.put("topP", 1);
        root.put("generationConfig", generationConfig);

        JSONArray safetySettings = new JSONArray();
        safetySettings.put(new JSONObject().put("category", "HARM_CATEGORY_HATE_SPEECH").put("threshold", "BLOCK_NONE"));
        safetySettings.put(new JSONObject().put("category", "HARM_CATEGORY_DANGEROUS_CONTENT").put("threshold", "BLOCK_NONE"));
        safetySettings.put(new JSONObject().put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT").put("threshold", "BLOCK_NONE"));
        safetySettings.put(new JSONObject().put("category", "HARM_CATEGORY_HARASSMENT").put("threshold", "BLOCK_NONE"));
        root.put("safetySettings", safetySettings);

        JSONObject retrieval = new JSONObject().put("vertexRagStore", new JSONObject()
            .put("ragResources", new JSONArray().put(new JSONObject()
                .put("ragCorpus", "projects/proj-newsshield-prod-infra/locations/us-central1/ragCorpora/6917529027641081856"))) // Update this if needed
            .put("ragRetrievalConfig", new JSONObject().put("topK", 20)));
        root.put("tools", new JSONArray().put(new JSONObject().put("retrieval", retrieval)));

        return root.toString();
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

 
    private ChatHistory getChatHistory(String userId) {
        ChatHistory history = historyCollection.find(Filters.eq("_id", userId)).first();
        if (history == null) {
            System.out.println("No history found for userId: " + userId + ". Creating new history.");
            return new ChatHistory(userId);
        }
        System.out.println("Found existing history for userId: " + userId);
        return history;
    }

    private void saveChatHistory(ChatHistory chatHistory) {
        ReplaceOptions options = new ReplaceOptions().upsert(true);
        historyCollection.replaceOne(Filters.eq("_id", chatHistory.getUserId()), chatHistory, options);
        System.out.println("Saved history for userId: " + chatHistory.getUserId());
    }
}