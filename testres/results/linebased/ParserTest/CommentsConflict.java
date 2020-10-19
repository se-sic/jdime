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
<<<<<<< .\CommentsLeft.java
     * @return all the comments
     */
    public final void getComments(final int num) {
=======
     * @return the list of comments
     */
    public void getComments(int num) {
>>>>>>> .\CommentsRight.java
        List<String> l = new ArrayList<>();

        for (int i = 0; i < num; i++) {
            l.add("Comment number " + i + "!");
        }

        return l;
    }
}
