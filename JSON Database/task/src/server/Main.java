package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final int PORT = 34522;
    public static volatile boolean shutdown = false;

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PORT)) {

            System.out.println("Server started!");
            ExecutorService executor = Executors.newFixedThreadPool(4);
            executor.submit(()->{
                while (true) {
                    if (shutdown) {
                        System.out.println(shutdown);
                        server.close();
                    }
                }
            });
            while (!shutdown) {
                try {
                    Socket socket = server.accept(); // accepting a new client
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    executor.submit(new ClientHandler(socket, input, output));


                } catch (Exception e) {
                    //System.out.println(e);
                }
            }
            executor.shutdown();

        } catch (IOException e) {
            //e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}


