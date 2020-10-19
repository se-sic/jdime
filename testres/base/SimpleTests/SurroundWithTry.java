public class SurroundWithTry {

    public void doSmth() {
        String s = ex();
    }

    private String ex() {
        throw new RuntimeException();
    }
}
