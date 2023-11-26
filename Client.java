import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Arrays;

public class Client implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;

    private boolean done;
    private Key sessionKey;

    public void shutdown() {
        done = true;
        try {
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            System.out.println("Error shutting down client");
        }
    }

    // Convert a String to a String array
    public static String[] stringToStringArray(String msg) {
        return Arrays.stream(msg.substring(1, msg.length() - 1).split(", "))
                .toArray(String[]::new);
    }

    // Convert a string array to a byte array (with the same values)
    public static byte[] stringArrayToByteArray(String[] msg) {
        byte[] messageBytes = new byte[msg.length];
        for (int i = 0; i < msg.length; i++) {
            messageBytes[i] = Byte.parseByte(msg[i]);
        }
        return messageBytes;
    }

    @Override
    public void run() {
        try {
            client = new Socket("localhost", 9999);
            // Create a keyPair
            KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

            // Send public key to server
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(client.getOutputStream());
            objectOutputStream.writeObject(keyPair.getPublic());
            objectOutputStream.flush();
            //System.out.println(keyPair.getPublic());

            // Receive public key from server
            ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
            PublicKey serverPublicKey = (PublicKey) objectInputStream.readObject();
            //System.out.println(serverPublicKey);

            // Receive the session key from the server
            byte[] encryptedSessionKey = (byte[]) objectInputStream.readObject();

            // Decrypt the session key
            Cipher cipherRSA = Cipher.getInstance("RSA");
            cipherRSA.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] sessionKeyBytes = cipherRSA.doFinal(encryptedSessionKey);

            // Transform sessionKeyBytes to a Key object
            sessionKey = new SecretKeySpec(sessionKeyBytes, 0, sessionKeyBytes.length, "AES");
            System.out.println("Session key: " + sessionKey);

            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);

            InputHandler inputHandler = new InputHandler();
            Thread thread = new Thread(inputHandler);
            thread.start();

            // Read from the server
            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                // System.out.println("Received message from server");
                // Decrypt the message with the session key
                Cipher cipherAES = Cipher.getInstance("AES");
                cipherAES.init(Cipher.DECRYPT_MODE, sessionKey);
                System.out.println("Message crypté: " + inMessage);
                System.out.println("Message décrypté: " + new String(cipherAES.doFinal(stringArrayToByteArray(stringToStringArray(inMessage)))));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Error connecting to server");
            shutdown();
        }
    }

    class InputHandler implements Runnable {

        @Override
        public void run() {
            try {
                // Read from the console
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while (!done) {
                    String message = inReader.readLine();
                    if (message.equals("bye")) {
                        out.println(message);
                        inReader.close();
                        shutdown();
                    } else {
                        // Encrypt the message with the session key
                        Cipher cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
                        byte[] encryptedMessage = cipher.doFinal(message.getBytes());
                        out.println(Arrays.toString(encryptedMessage));

                        /* Useful for debugging
                        System.out.println(Arrays.toString(encryptedMessage));
                        System.out.println(message);
                        */
                    }
                }
            } catch (Exception e) {
                System.out.println("Error reading from console");
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
