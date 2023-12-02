import java.util.Arrays;

public class Test {

    public Test() {
        Server server = new Server();
        Client client = new Client();
        ChatGUI chatGUI = new ChatGUI(client);
        client.setChatGUI(chatGUI);
        Client client2 = new Client();
        ChatGUI chatGUI2 = new ChatGUI(client2);
        client2.setChatGUI(chatGUI2);
    }


    public static void main(String[] args) {
        new Test();
    }
}
