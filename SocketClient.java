import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class SocketClient {

    public InetAddress address;
    public Socket socket;

    public SocketClient() throws IOException {
        this.address = InetAddress.getLocalHost();
        this.socket = new Socket("localhost", 8080);
    }

    public static void main(String[] args) {

    }
}
