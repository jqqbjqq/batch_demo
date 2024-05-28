package com;
import java.io.*;
import java.net.*;

public class SimpleFirewall {
    public static void main(String[] args) {
        int port = 8080;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Firewall is listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String line;
                boolean allowed = true;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("BLOCKED")) {
                        allowed = false;
                        break;
                    }
                }

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                if (allowed) {
                    writer.println("Connection allowed");
                } else {
                    writer.println("Connection blocked");
                }
                socket.close();
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}