package client;

import com.beust.jcommander.JCommander;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Main {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 34522;

    public static void main(String[] args) {
        Args jArgs = new Args();
        JCommander.newBuilder()
                .addObject(jArgs)
                .build()
                .parse(args);
        try (
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output  = new DataOutputStream(socket.getOutputStream())
        ) {
            if (jArgs.getFileName() == null) {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder
                        .excludeFieldsWithoutExposeAnnotation()
                        .create();
                String jsonOutput = gson.toJson(jArgs);
                System.out.println("Client started!");
                output.writeUTF(jsonOutput); // sending message to the server
                System.out.printf("Sent: %s%n", jsonOutput);
                String receivedMsg = input.readUTF(); // response message
                System.out.printf("Received: %s%n", receivedMsg);
            } else {
                Path path = Paths.get("src/client/data/" + jArgs.getFileName());

                String jsonString = readFileAsString(path);

                System.out.println("Client started!");
                output.writeUTF(jsonString); // sending message to the server
                System.out.printf("Sent: %n%s%n", jsonString);
                String receivedMsg = input.readUTF(); // response message
                System.out.printf("Received: %s%n", receivedMsg);

            }
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String readFileAsString(Path path)throws Exception
    {
        return new String(Files.readAllBytes(path));
    }
}
