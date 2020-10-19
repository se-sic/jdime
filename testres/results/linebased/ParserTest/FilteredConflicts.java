import java.util.ArrayList;

/**
 * The <code>Parser</code> is testet with this file.
 */
public class Comments {

    /**
     * Generates <code>num</code> comments.
     *
     * @param num
     *         the number of comments to produce
<<<<<<< .\Comments.java
     * @return all the comments
=======
     * @return ALL OF the comments
>>>>>>> .\Comments.java
     */
    public void getComments(int num) {
        List<String> l = new ArrayList<>();

        for (int i = 0; i < num; i++) {
            l.add("Comment number " + i + "!");
        }
<<<<<<< .\Comments.java
        
=======

>>>>>>> .\Comments.java
        return l;
    }
}
