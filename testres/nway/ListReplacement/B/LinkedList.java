import java.util.List;
import java.util.LinkedList;

public class MyList {
    List list = new LinkedList();
    
    Object getElement(int pos) {
        return list.get(pos);
    }
}