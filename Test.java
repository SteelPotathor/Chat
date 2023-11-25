import java.util.Arrays;

public class Test {

    public static void main(String[] args) {
        // Mauvaise conversion
        byte[] messageBytes = {0x4, 0x5, 0x6};
        String[] msg = {"4", "5", "6"};
        System.out.println(Arrays.toString(messageBytes));
        System.out.println(Arrays.toString(msg));
        System.out.println(Arrays.toString(Arrays.toString(msg).getBytes()));
    }
}
