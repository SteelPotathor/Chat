import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatGUI extends JFrame implements KeyListener, MouseListener {
    public JTextArea chatArea;
    private final JTextField inputField;
    private final Client client;

    /**
     * Create a chat GUI
     *
     * @param client the client
     */
    public ChatGUI(Client client) {
        // Create the GUI
        setTitle("Simple Chat");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.client = client;

        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        inputField.addKeyListener(this);

        JButton sendButton = new JButton("Send");
        sendButton.addMouseListener(this);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(inputPanel, BorderLayout.SOUTH);
        this.setVisible(true);
    }

    /**
     * Send a message (called when the user presses enter or clicks the send button)
     */
    private void sendMessage() {
        String message = inputField.getText();
        // Don't send empty messages
        if (!message.trim().isEmpty()) {
            // Reset the input field
            inputField.setText("");

            // Send the message to the server
            client.inputHandler.sendMessage(message);
        }
    }

    /**
     * Append a message to the chat area
     *
     * @param msg the message
     */
    public void appendMessage(String msg) {
        // Append the message to the chat area (ensure thread safety)
        SwingUtilities.invokeLater(() -> chatArea.append(msg));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client(9999);
            ChatGUI chatGUI = new ChatGUI(client);
            client.setChatGUI(chatGUI);
            new Thread(client).start();
        });

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Enter key
        if ((e.getKeyCode() == KeyEvent.VK_ENTER)) {
            sendMessage();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Left click
        if ((e.getButton() == MouseEvent.BUTTON1)) {
            sendMessage();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}

