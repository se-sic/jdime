import java.util.List;
// #ifdef A || C
import java.util.ArrayList;
// #elif B
import java.util.LinkedList;
// #endif

public class MyList {
    List list = new 
    // #ifdef A || C
    ArrayList
    // #elif B
    LinkedList
    // #endif
    ();
    
    Object getElement(int 
    // #ifdef A || C
    position
    // #elif B
    pos
    // #endif
    ) {
        return list.get(
        // #ifdef A
        position
        // #elif B
        pos
        // #elif C
        position - 1
        // #endif
        );
    }
}