public class SWLoop {

    public boolean isOnline() {
        boolean online;

        int i = 0;
        while (i < 10) {
            online = check();
            i++;
        }

        return online;
    }

    public boolean check() {
        return true;
    }
}