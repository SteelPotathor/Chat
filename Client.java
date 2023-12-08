import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.Arrays;

public class Client implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;
    private KeyPair keyPair;
    private Key sessionKey;
    public InputHandler inputHandler;
    public ChatGUI chatGUI;


    /**
     * Create a client
     *
     * @param port the port
     */
    public Client(int port) {
        done = false;
        try {
            // Connect to the server
            client = new Socket("localhost", port);

            // Create a keyPair
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

            // Create the input and output streams
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error connecting to server");
            shutdown();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error generating keyPair");
            shutdown();
        }
    }

    /**
     * Shutdown the client
     */
    public void shutdown() {
        System.out.println("Shutting down client");
        done = true;
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (client != null && !client.isClosed()) {
                client.close();
            }
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Error shutting down client");
        }
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
     * Set the chatGUI
     *
     * @param chatGUI the chatGUI
     */
    public void setChatGUI(ChatGUI chatGUI) {
        this.chatGUI = chatGUI;
    }

    /**
     * Send a message to the GUI (append it to the chat area)
     *
     * @param message the message
     */
    public void sendMessageToGUI(String message) {
        String msg = message + "\n";
        chatGUI.appendMessage(msg);
    }

    /**
     * Run the client
     */
    @Override
    public void run() {
        try {
            // Send public key to server
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(client.getOutputStream());
            objectOutputStream.writeObject(keyPair.getPublic());
            objectOutputStream.flush();

            // Create an object input stream
            ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());

            // Receive the session key from the server
            byte[] encryptedSessionKey = (byte[]) objectInputStream.readObject();

            // Decrypt the session key
            Cipher cipherRSA = Cipher.getInstance("RSA");
            cipherRSA.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] sessionKeyBytes = cipherRSA.doFinal(encryptedSessionKey);

            // Transform sessionKeyBytes to a Key object
            sessionKey = new SecretKeySpec(sessionKeyBytes, 0, sessionKeyBytes.length, "AES");
            System.out.println("Session key: " + sessionKey);

            // Create an input handler
            inputHandler = new InputHandler();

            // Start the input handler thread
            Thread thread = new Thread(inputHandler);
            thread.start();

            // Read from the server
            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                // Decrypt the message with the session key
                Cipher cipherAES = Cipher.getInstance("AES");
                cipherAES.init(Cipher.DECRYPT_MODE, sessionKey);

                String decryptedMessage = new String(cipherAES.doFinal(stringArrayToByteArray(stringToStringArray(inMessage))));
                // Print in the console
                System.out.println("Message crypté: " + inMessage);
                System.out.println("Message décrypté: " + decryptedMessage);

                sendMessageToGUI(decryptedMessage);
            }
            // Close the client if the server is down
            shutdown();
        } catch (Exception e) {
            System.out.println("Error running client");
            shutdown();
        }
    }

    class InputHandler implements Runnable {

        private Cipher cipher;

        /**
         * Constructor for the input handler
         */
        public InputHandler() {
            try {
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
            } catch (Exception e) {
                System.out.println("Error initializing cipher");
                shutdown();
            }
        }

        /**
         * Send a message to the server (encrypted with the session key)
         *
         * @param message the message
         */
        public void sendMessage(String message) {
            try {
                // Encrypt the message with the session key
                byte[] encryptedMessage = cipher.doFinal(message.getBytes());
                // Send the message
                out.println(Arrays.toString(encryptedMessage));
            } catch (Exception e) {
                // System.out.println(e.getMessage());
                System.out.println("Error sending message");
                shutdown();
            }
        }

        /**
         * Run the input handler
         */
        @Override
        public void run() {
            try {
                // Read from the console
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));

                while (!done) {
                    String message = inReader.readLine();

                    // Send the message to the server (encrypted with the session key)
                    sendMessage(message);
                    if (message.equals("/bye")) {
                        inReader.close();
                        shutdown();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error reading from console");
                shutdown();
            }
        }
    }
}
