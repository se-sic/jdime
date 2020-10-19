public class SurroundWithTry {

    public void doSmth() {
<<<<<<< .\left\SimpleTests\SurroundWithTry.java
        try {
            String s = ex();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
=======
        String s = ex();
        System.out.println(s);
>>>>>>> .\right\SimpleTests\SurroundWithTry.java
    }

    private String ex() {
        throw new RuntimeException();
    }
}