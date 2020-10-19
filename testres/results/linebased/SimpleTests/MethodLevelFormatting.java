import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MethodLevelFormatting {

    public void method() {

        if (Math.random() > .6f) {
            System.out.println("40% chance!");
        }

<<<<<<< /usr/local/code/JDime/build/jdime-testfiles/left/SimpleTests/MethodLevelFormatting.java
        try (OutputStream out = new FileOutputStream("out")) { out.write(21); } catch (IOException e) {
=======
        try (OutputStream out = new FileOutputStream("out")) {
            out.write(42);
        } catch (IOException e) {
>>>>>>> /usr/local/code/JDime/build/jdime-testfiles/right/SimpleTests/MethodLevelFormatting.java
            e.printStackTrace();
        }
    }
}
