import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class AnonClassMovedMethod {

    public MouseListener getListener() {
        return new MouseListener() {

            @Override
<<<<<<< /usr/local/code/JDime/build/jdime-testfiles/threeway/left/SimpleTests/AnonClassMovedMethod.java
            public void mouseClicked(MouseEvent e) {
                System.out.println("Mouse Clicked!");
            }

            @Override
=======
>>>>>>> /usr/local/code/JDime/build/jdime-testfiles/threeway/right/SimpleTests/AnonClassMovedMethod.java
            public void mousePressed(MouseEvent e) {
                System.out.println("Mouse Pressed!");
            }

            @Override
<<<<<<< /usr/local/code/JDime/build/jdime-testfiles/threeway/left/SimpleTests/AnonClassMovedMethod.java
            public void mouseReleased(MouseEvent e) {
                System.out.println("Mouse Released!");
=======
            public void mouseClicked(MouseEvent e) {
                System.out.println("Mouse Clicked!");
>>>>>>> /usr/local/code/JDime/build/jdime-testfiles/threeway/right/SimpleTests/AnonClassMovedMethod.java
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                System.out.println("Mouse Entered!");
            }

            @Override
<<<<<<< /usr/local/code/JDime/build/jdime-testfiles/threeway/left/SimpleTests/AnonClassMovedMethod.java
=======
            public void mouseReleased(MouseEvent e) {
                System.out.println("Mouse Released!");
            }

            @Override
>>>>>>> /usr/local/code/JDime/build/jdime-testfiles/threeway/right/SimpleTests/AnonClassMovedMethod.java
            public void mouseExited(MouseEvent e) {
                System.out.println("Mouse Exited!");
            }
        };
    }
}
