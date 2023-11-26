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

    private final ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
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
            KeyPair serverKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
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

                ConnectionHandler handler = new ConnectionHandler(client, sessionKey);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            System.out.println("Error running server");
            shutdown();
        }
    }

    class ConnectionHandler implements Runnable {

        private final Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private final Key sessionKey;
        private Cipher cipher;

        public ConnectionHandler(Socket client, Key sessionKey) {
            this.client = client;
            this.sessionKey = sessionKey;
            try {
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, sessionKey);
            } catch (Exception e) {
                System.out.println("Error creating cipher");
            }
        }

        public void sendMessage(String message) {
            // Encrypt the message
            try {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
                byte[] encryptedMessage = cipher.doFinal(message.getBytes());
                // Send the message
                out.println(Arrays.toString(encryptedMessage));
                // System.out.println(Arrays.toString(encryptedMessage));
            } catch (Exception e) {
                System.out.println("Error encrypting message");
            }
        }

        // This method does not work properly further test must be done
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

        // Convert a String to a String array
        public String[] stringToStringArray(String msg) {
            return Arrays.stream(msg.substring(1, msg.length() - 1).split(", "))
                    .toArray(String[]::new);
        }

        // Convert a string array to a byte array (with the same values)
        public byte[] stringArrayToByteArray(String[] msg) {
            byte[] messageBytes = new byte[msg.length];
            for (int i = 0; i < msg.length; i++) {
                messageBytes[i] = Byte.parseByte(msg[i]);
            }
            return messageBytes;
        }

        public String decryptMessage(String message) {
            try {
                // Decrypt the message
                return new String(cipher.doFinal(stringArrayToByteArray(stringToStringArray(message))));
            } catch (Exception e) {
                System.out.println("Error decrypting message");
                return null;
            }
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                sendMessage("Please enter a nickname: ");

                String nickname = decryptMessage(in.readLine());
                System.out.println(nickname + " connected");
                broadcast(nickname + " joined the chat!");
                String message;
                while ((message = in.readLine()) != null) {
                    // Decrypt the message
                    String clearMessage = decryptMessage(message);

                    // Console of the server
                    System.out.println(nickname + ": " + message);
                    System.out.println(nickname + ": " + clearMessage);

                    if (clearMessage.startsWith("/nick ")) {
                        String[] messageSplit = clearMessage.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(nickname + " changed their nickname to " + messageSplit[1]);
                            System.out.println(nickname + " changed their nickname to " + messageSplit[1]);
                            nickname = messageSplit[1];
                            sendMessage("Nickname changed to " + nickname);
                        } else {
                            sendMessage("Invalid nickname");
                        }
                    } else if (clearMessage.equals("bye")) {
                        broadcast(nickname + " left the chat");
                        //shutdown(); this method does not work properly
                    } else {
                        broadcast(nickname + ": " + clearMessage);
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("Error handling client");
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
