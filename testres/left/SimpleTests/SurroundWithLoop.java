public class SWLoop {

    public boolean isOnline() {
        boolean online;

        for (int i = 0; i < 10; i++) {
            online = check();
        }

        return online;
    }

    public boolean check() {
        return true;
    }
}