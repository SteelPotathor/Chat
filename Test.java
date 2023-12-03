import java.util.Arrays;

public class Test {

    public static String[] stringToStringArray(String msg) {
        return Arrays.stream(msg.substring(1, msg.length() - 1).split(", "))
                .toArray(String[]::new);
    }


    public static void main(String[] args) {
        System.out.println(Arrays.toString(stringToStringArray("[1, 2, 3, 4, 5]")));
    }
}
