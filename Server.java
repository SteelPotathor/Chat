import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple chat server
 */
public class Server implements Runnable {

    private final ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;
    private KeyPair serverKeyPair;

    /**
     * Constructor
     */
    public Server() {
        connections = new ArrayList<>();
        done = false;
        try {
            // Create a server socket on port 9999
            server = new ServerSocket(9999);

            // Create a keyPair for the server using RSA
            serverKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (IOException e) {
            System.out.println("Error creating server socket");
            shutdown();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error creating key pair");
            shutdown();
        }

        // Create a thread pool to handle the connections with the clients
        pool = Executors.newCachedThreadPool();
        System.out.println("Server started");
        System.out.println("Waiting for clients...");
    }

    /**
     * Broadcast a message to all the clients (it will be encrypted with the session key of each client)
     *
     * @param message the message to broadcast
     */
    public void broadcast(String message) {
        for (ConnectionHandler connection : connections) {
            if (connection != null) {
                connection.sendMessage(message);
            }
        }
    }

    /**
     * Shutdown the server and close all the connections with the clients
     */
    public void shutdown() {
        done = true;
        if (pool != null) {
            pool.shutdown();
        }
        // Close all the connections
        for (ConnectionHandler connection : connections) {
            if (connection != null) {
                connection.sendMessage("Server shutting down");
                connection.shutdown();
            }
        }
        try {
            if (server != null && !server.isClosed()) {
                server.close();
            }
        } catch (IOException e) {
            System.out.println("Error shutting down server");
        }
    }

    /**
     * Run the server
     */
    @Override
    public void run() {
        // Wait for clients to connect
        while (!done) {
            try {
                // Accept a client
                Socket client = server.accept();

                // Send the public key to the client
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(client.getOutputStream());
                objectOutputStream.writeObject(serverKeyPair.getPublic());
                objectOutputStream.flush();

                // Receive the public key from the client
                ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
                PublicKey clientPublicKey = (PublicKey) objectInputStream.readObject();
                // System.out.println("Client connected" + clientPublicKey);

                // Create a session key using AES
                Key sessionKey = KeyGenerator.getInstance("AES").generateKey();
                //System.out.println("Session key: " + sessionKey);

                // Encrypt the session key using the client's public key
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);
                byte[] encryptedSessionKey = cipher.doFinal(sessionKey.getEncoded());

                // Send the encrypted session key to the client
                objectOutputStream.writeObject(encryptedSessionKey);
                objectOutputStream.flush();

                // Create a connection handler for the client (with the session key) and add it to the thread pool (it will run in a separate thread)
                ConnectionHandler handler = new ConnectionHandler(client, sessionKey);
                connections.add(handler);
                pool.execute(handler);
            } catch (Exception e) {
                System.out.println("Error accepting client");
            }
        }
    }

    /**
     * A class to handle the connection with a client
     */
    class ConnectionHandler implements Runnable {

        private final Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private final Key sessionKey;
        private Cipher cipher;
        private String nickname;

        /**
         * Constructor
         *
         * @param client     the client socket
         * @param sessionKey the session key
         */
        public ConnectionHandler(Socket client, Key sessionKey) {
            this.client = client;
            this.sessionKey = sessionKey;
            try {
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, sessionKey);
            } catch (Exception e) {
                System.out.println("Error creating cipher");
                shutdown();
            }
        }


        /**
         * Convert a string array to a string (with the spaces)
         *
         * @param msg   the string array
         * @param start the start index
         * @param end   the end index
         * @return the string
         */
        private String stringArrayToString(String[] msg, int start, int end) {
            StringBuilder message = new StringBuilder();
            for (int i = start; i < end; i++) {
                message.append(msg[i]).append(" ");
            }
            return message.toString();
        }

        /**
         * Convert a string (which looks like this: [1, 2, 3, 4]) to a string array
         *
         * @param msg the string array
         * @return the string
         */
        public String[] stringToStringArray(String msg) {
            return Arrays.stream(msg.substring(1, msg.length() - 1).split(", "))
                    .toArray(String[]::new);
        }

        /**
         * Convert a string array to a byte array (with the same values)
         *
         * @param msg the string array
         * @return the byte array
         */
        public byte[] stringArrayToByteArray(String[] msg) {
            byte[] messageBytes = new byte[msg.length];
            for (int i = 0; i < msg.length; i++) {
                messageBytes[i] = Byte.parseByte(msg[i]);
            }
            return messageBytes;
        }

        /**
         * Send a message to the client (it will be encrypted with the session key)
         *
         * @param message the message to send
         */
        public void sendMessage(String message) {
            try {
                // Encrypt the message with the session key
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
                byte[] encryptedMessage = cipher.doFinal(message.getBytes());

                // Send the encrypted message
                out.println(Arrays.toString(encryptedMessage));
            } catch (Exception e) {
                System.out.println("Error encrypting message");
            }
        }

        /**
         * Decrypt a message with the session key
         *
         * @param message the message to decrypt
         * @return the decrypted message
         */
        public String decryptMessage(String message) {
            try {
                // Decrypt the message
                return new String(cipher.doFinal(stringArrayToByteArray(stringToStringArray(message))));
            } catch (Exception e) {
                System.out.println("Error decrypting message");
                return null;
            }
        }

        /**
         * Shutdown the client connection
         */
        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing connection with client");
            }
        }

        @Override
        public void run() {
            try {
                // Create the input and output streams
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                // Ask the client for a nickname
                sendMessage("Please enter a nickname: ");

                // Receive the nickname from the client
                nickname = decryptMessage(in.readLine());
                System.out.println(nickname + " connected");

                // Broadcast the nickname to the other clients
                broadcast(nickname + " joined the chat!");

                String message;
                // Read from the client
                while ((message = in.readLine()) != null) {
                    // Decrypt the message
                    String clearMessage = decryptMessage(message);

                    // Console of the server
                    System.out.println(nickname + ": " + message);
                    System.out.println(nickname + ": " + clearMessage);

                    // Commands
                    if (clearMessage.startsWith("/nick")) {
                        String[] messageSplit = clearMessage.split(" ", 2);
                        if (messageSplit.length >= 2) {
                            String newNickname = stringArrayToString(messageSplit, 1, messageSplit.length);

                            // Broadcast the nickname change to the other clients
                            broadcast(nickname + " changed their nickname to " + newNickname);
                            System.out.println(nickname + " changed their nickname to " + newNickname);

                            // Change the nickname
                            nickname = messageSplit[1];
                            // Alert the client
                            sendMessage("Nickname changed to " + nickname);
                        } else {
                            sendMessage("The nickname cannot be empty");
                        }
                    } else if (clearMessage.startsWith("/mp")) {
                        String[] messageSplit = clearMessage.split(" ");
                        if (messageSplit.length >= 3) {
                            String receiver = messageSplit[1];
                            String msg = stringArrayToString(messageSplit, 2, messageSplit.length);
                            boolean userFound = false;
                            for (ConnectionHandler connection : connections) {
                                if (connection != null && connection != this && connection.nickname.equals(receiver)) {
                                    // Send the private message to the receiver
                                    connection.sendMessage(nickname + " (private): " + msg);
                                    System.out.println(nickname + " (private): " + msg);
                                    userFound = true;
                                    break;
                                }
                            }
                            if (!userFound) {
                                this.sendMessage("User not found");
                            } else {
                                this.sendMessage("Private message sent to " + receiver);
                            }
                        } else {
                            sendMessage("You must specify a receiver and a message");
                        }
                    } else if (clearMessage.equals("/bye")) {
                        broadcast(nickname + " left the chat");
                        System.out.println(nickname + " left the chat");
                        shutdown();
                    } else {
                        // If the message is not a command, broadcast it to the other clients
                        broadcast(nickname + ": " + clearMessage);
                    }
                }
            } catch (IOException e) {
                if (e.getMessage().equals("Stream closed")) {
                    System.out.println("Client disconnected");
                } else {
                    System.out.println("Error running connection handler");
                    shutdown();
                }
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
