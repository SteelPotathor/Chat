import java.util.Arrays;

public class Test {

    byte[] convert(String[] msg) {
        byte[] messageBytes = new byte[msg.length];
        for (int i = 0; i < msg.length; i++) {
            messageBytes[i] = Byte.parseByte(msg[i]);
        }
        return messageBytes;
    }

    // String to String[] with java.stream

    String[] convert2(String msg) {
        String[] messageBytes = new String[msg.length()];
        for (int i = 0; i < msg.length(); i++) {
            messageBytes[i] = String.valueOf(msg.charAt(i));
        }
        return messageBytes;
    }

    // String to String[] with java.stream
    String[] convert3(String msg) {
        return Arrays.stream(msg.substring(1, msg.length() - 1).split(", "))
                .toArray(String[]::new);
    }

    String stringArrayToString(String[] msg) {
        // remove the first two elements

        StringBuilder message = new StringBuilder();
        for (int i = 0; i < msg.length; i++) {
            message.append(msg[i]);
        }
        return message.toString();
    }

    // string array to string but remove the first two elements

    public static void main(String[] args) {
        Test t = new Test();
        // Mauvaise conversion
        byte[] messageBytes = {0x4, 0x5, 0x6};
        String[] msg = {"4", "5", "6"};
        System.out.println(Arrays.toString(messageBytes));
        System.out.println(Arrays.toString(msg));
        System.out.println(Arrays.toString(Arrays.toString(msg).getBytes()));
        System.out.println(Arrays.toString(t.convert(msg)));
        System.out.println(Arrays.toString(t.convert2("[4, 5, 6]")));
        System.out.println(Arrays.toString(t.convert3("[4, 5, 6]")));
        System.out.println(t.stringArrayToString(msg));
    }
}
