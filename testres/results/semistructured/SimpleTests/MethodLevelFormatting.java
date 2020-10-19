import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MethodLevelFormatting {
  public void method() {

        if (Math.random() > .6f) {
            System.out.println("40% chance!");
        }

<<<<<<<
        try (OutputStream out = new FileOutputStream("out")) { out.write(21); } catch (IOException e) {
=======
        try (OutputStream out = new FileOutputStream("out")) {
            out.write(42);
        } catch (IOException e) {
>>>>>>>
            e.printStackTrace();
        }
  }
}