import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;
    private KeyPair serverKeyPair;

    public Server() {
        connections = new ArrayList<>();
        done = false;
        try {
            // Generate RSA key pair (public/private)
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            serverKeyPair = keyPairGen.generateKeyPair();
        } catch (Exception e) {
            System.out.println("Error generating server keys");
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler connection : connections) {
            if (connection != null) {
                connection.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler connection : connections) {
                if (connection != null) {
                    connection.sendMessage("Server shutting down");
                    connection.shutdown();
                }
            }
        } catch (IOException e) {
            System.out.println("Error shutting down server");
        }
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            // Create a keyPair
            serverKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
            //System.out.println("Server public key: " + serverKeyPair.getPublic());
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                // Send the public key to the client
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(client.getOutputStream());
                objectOutputStream.writeObject(serverKeyPair.getPublic());
                objectOutputStream.flush();

                // Receive the public key from the client
                ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
                PublicKey clientPublicKey = (PublicKey) objectInputStream.readObject();
                //System.out.println("Client connected" + clientPublicKey);

                // Crypt the session key with the client's public key
                Key sessionKey = KeyGenerator.getInstance("AES").generateKey();
                //System.out.println("Session key: " + sessionKey);
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);
                byte[] encryptedSessionKey = cipher.doFinal(sessionKey.getEncoded());

                // Send the encrypted session key to the client
                objectOutputStream.writeObject(encryptedSessionKey);
                objectOutputStream.flush();

                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing connection");
            }
        }

        @Override
        public void run() {
            try {

                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a nickname: ");
                nickname = in.readLine();
                System.out.println(nickname + " connected");
                broadcast(nickname + " joined the chat!");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(nickname + " changed their nickname to " + messageSplit[1]);
                            System.out.println(nickname + " changed their nickname to " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Nickname changed to " + nickname);
                        } else {
                            out.println("Invalid nickname");
                        }
                    } else if (message.equals("bye")) {
                        broadcast(nickname + " left the chat");
                        shutdown();
                    } else
                        broadcast(nickname + ": " + message);
                }
            } catch (Exception e) {
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
