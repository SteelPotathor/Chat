import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatGUI extends JFrame implements KeyListener, MouseListener {
    public JTextArea chatArea;
    private final JTextField inputField;
    private final Client client;

    public ChatGUI(Client client) {
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

    // Send a message to the server
    private void sendMessage() {
        String message = inputField.getText();
        if (!message.trim().isEmpty()) {
            inputField.setText("");
            client.inputHandler.sendMessage(message);
        }
    }

    // Append a message to the chat area
    public void appendMessage(String msg) {
        chatArea.append(msg);
    }

    public static void main(String[] args) {
        Client client = new Client(9999);
        ChatGUI chatGUI = new ChatGUI(client);
        client.setChatGUI(chatGUI);
        client.run();
        System.exit(0);
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

