package com.chatbot.vertex.model;

import org.bson.codecs.pojo.annotations.BsonId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatHistory {

    @BsonId
    private String userId;
    private List<Content> history;

    public ChatHistory() {
        this.history = new ArrayList<>();
    }

    public ChatHistory(String userId) {
        this.userId = userId;
        this.history = new ArrayList<>();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<Content> getHistory() { return history; }
    public void setHistory(List<Content> history) { this.history = history; }

    public void addMessage(String role, String text) {
        Part part = new Part(text);
    
        Content content = new Content(role, Arrays.asList(part));
                                   
        this.history.add(content);
    }
}