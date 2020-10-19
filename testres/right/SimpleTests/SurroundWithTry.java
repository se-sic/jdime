public class SurroundWithTry {

    public void doSmth() {
        String s = ex();
        System.out.println(s);
    }

    private String ex() {
        throw new RuntimeException();
    }
}
