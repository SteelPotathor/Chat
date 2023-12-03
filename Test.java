import java.util.Arrays;

public class Test {




    public static void main(String[] args) {
        boolean flag = true;
        while (flag) {
            System.out.println("Hello");
            try {
                Thread.sleep(1000);
                throw new InterruptedException();
            } catch (InterruptedException e) {
                System.out.println("Error");
            }
        }
    }
}
