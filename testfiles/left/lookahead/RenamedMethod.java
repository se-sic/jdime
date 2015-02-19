import java.util.LinkedList;
class Stack {
    LinkedList<Integer> mylist = new LinkedList<>();
    public int pop() { return mylist.pop(); }
    public void push(int i) { mylist.push(i); }
    public int size() { return mylist.size(); }
}
