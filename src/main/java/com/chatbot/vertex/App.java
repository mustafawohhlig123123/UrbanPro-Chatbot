package com.chatbot.vertex;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.cloud.aiplatform.v1.EndpointName;
import com.google.cloud.aiplatform.v1.PredictRequest;
import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import io.github.cdimascio.dotenv.Dotenv;

import static java.util.stream.Collectors.toList;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Paths;






public class App {

    // private static final int PORT = 7070;
    // private static final String SERVER_URL = "http://localhost:" + PORT + "/chat";

    // public static void main(String[] args) {
    //     ChatServer server = new ChatServer(PORT);
    //     try {
            
    //         server.start();

    //         Thread.sleep(2000);

    //         ChatClient client = new ChatClient(SERVER_URL);
    //         client.runConsoleSession();

    //     } catch (InterruptedException e) {
    //         System.err.println("Application was interrupted.");
    //         Thread.currentThread().interrupt(); 
    //     } finally {
  
    //         server.stop();
    //         System.out.println("Application has shut down gracefully.");
    //     }
    // }
    public static void main(String[] args) throws IOException {
        // Scanner scanner = new Scanner(System.in);
        // System.out.print("Enter the path to the PDF file: ");    // path to your PDF file
        // String pdfPath = scanner.nextLine();  
        // scanner.close(); 
        // String markdownPath = "output.md";      // desired output file

        // try (PDDocument document = PDDocument.load(new File(pdfPath))) {
        //     PDFTextStripper pdfStripper = new PDFTextStripper();
        //     String text = pdfStripper.getText(document);

        //     String markdown = text;

        //     try (BufferedWriter writer = new BufferedWriter(new FileWriter(markdownPath))) {
        //         writer.write(markdown);
        //     }

        //     System.out.println("Conversion done. Saved to " + markdownPath);

        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
    
//     Dotenv dotenv = Dotenv.load();
//     String endpoint = dotenv.get("endpoint"); 
//     String project = dotenv.get("PROJECT_ID");
//     String model = "gemini-embedding-001";
//     // Java 8–compatible list
//     // List<String> texts = Files.readAllLines(Paths.get("output.md"));
//     List<String> texts = Arrays.asList("my name is mustafa");
//     System.out.println("Texts: " + texts);

//     List<List<Float>> embeddings = predictTextEmbeddings(
//         endpoint,
//         project,
//         model,
//         texts,
//         "QUESTION_ANSWERING",
//         OptionalInt.of(3072));

//     // print sizes
//     for (List<Float> emb : embeddings) {
//         System.out.println("Embeddings: " + emb);

//       }
//   }

//   // Gets text embeddings from a pretrained, foundational model.
//   public static List<List<Float>> predictTextEmbeddings(
//       String endpoint,
//       String project,
//       String model,
//       List<String> texts,
//       String task,
//       OptionalInt outputDimensionality)
//       throws IOException {
//     PredictionServiceSettings settings =
//         PredictionServiceSettings.newBuilder().setEndpoint(endpoint).build();
//     Matcher matcher = Pattern.compile("^(?<Location>\\w+-\\w+)").matcher(endpoint);
//     String location = matcher.matches() ? matcher.group("Location") : "us-central1";
//     EndpointName endpointName =
//         EndpointName.ofProjectLocationPublisherModelName(project, location, "google", model);

//     List<List<Float>> floats = new ArrayList<>();
//     // You can use this prediction service client for multiple requests.
//     try (PredictionServiceClient client = PredictionServiceClient.create(settings)) {
//       // gemini-embedding-001 takes one input at a time.
//       for (int i = 0; i < texts.size(); i++) {
//         PredictRequest.Builder request = 
//             PredictRequest.newBuilder().setEndpoint(endpointName.toString());
//         if (outputDimensionality.isPresent()) {
//           request.setParameters(
//               Value.newBuilder()
//                   .setStructValue(
//                       Struct.newBuilder()
//                           .putFields(
//                               "outputDimensionality", valueOf(outputDimensionality.getAsInt()))
//                           .build()));
//         }
//         request.addInstances(
//             Value.newBuilder()
//                 .setStructValue(
//                     Struct.newBuilder()
//                         .putFields("content", valueOf(texts.get(i)))
//                         .putFields("task_type", valueOf(task))
//                         .build()));
//         PredictResponse response = client.predict(request.build());

//         for (Value prediction : response.getPredictionsList()) {
//           Value embeddings = prediction.getStructValue().getFieldsOrThrow("embeddings");
//           Value values = embeddings.getStructValue().getFieldsOrThrow("values");
//           floats.add(
//               values.getListValue().getValuesList().stream()
//                   .map(Value::getNumberValue)
//                   .map(Double::floatValue)
//                   .collect(toList()));
//         }
//       }
//       return floats;
//     }
//   }

//   private static Value valueOf(String s) {
//     return Value.newBuilder().setStringValue(s).build();
//   }

//   private static Value valueOf(int n) {
//     return Value.newBuilder().setNumberValue(n).build();
//   }
}
