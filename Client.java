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
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] sessionKeyBytes = cipher.doFinal(encryptedSessionKey);

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
                System.out.println(inMessage);
            }
        } catch (Exception e) {
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
