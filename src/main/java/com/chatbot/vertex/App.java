package com.chatbot.vertex;

public class App {

    private static final int PORT = 7070;
    private static final String SERVER_URL = "http://localhost:" + PORT + "/chat";

    public static void main(String[] args) {
        ChatServer server = new ChatServer(PORT);
        try {
            
            server.start();

            Thread.sleep(2000);

            ChatClient client = new ChatClient(SERVER_URL);
            client.runConsoleSession();

        } catch (InterruptedException e) {

            System.err.println("Application was interrupted.");
            Thread.currentThread().interrupt(); 
            
        } finally {
  
            server.stop();
            System.out.println("Application has shut down gracefully.");
        }
    }
}