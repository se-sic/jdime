import java.util.List;
import java.util.ArrayList;

public class MyList {
    List list = new ArrayList();
    
    Object getElement(int position) {
        return list.get(position);
    }
}