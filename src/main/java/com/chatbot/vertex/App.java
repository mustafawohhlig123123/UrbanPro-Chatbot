package com.chatbot.vertex;

public class App {

    private static final int PORT = 7070;
    private static final String SERVER_URL = "http://localhost:" + PORT + "/chat";

    public static void main(String[] args) {
        ChatServer server = new ChatServer(PORT);
        
        // Use a try-finally block to ensure the server is always stopped.
        try {
            // 1. Start the server in the background
            server.start();

            // 2. Give the server a moment to start up before the client tries to connect
            Thread.sleep(2000);

            // 3. Create a client and run the console session in the foreground
            ChatClient client = new ChatClient(SERVER_URL);
            client.runConsoleSession();

        } catch (InterruptedException e) {
            System.err.println("Application was interrupted.");
            Thread.currentThread().interrupt(); // Restore the interrupted status
        } finally {
            // 4. Stop the server when the client console exits
            server.stop();
            System.out.println("Application has shut down gracefully.");
        }
    }
}