import java.util.LinkedList;
class MovedNode {
    LinkedList<Integer> mylist = new LinkedList<>();
    public int pop() { return mylist.pop(); }
    public void push(int i) { mylist.push(i); }
    public void clear() { if(mylist.size()>0) mylist.clear(); }
}
