public class SurroundWithTry {

    public void doSmth() {
        try {
            String s = ex();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private String ex() {
        throw new RuntimeException();
    }
}
