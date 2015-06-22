import java.util.List;
import java.util.LinkedList;

class Stack {
	List<Integer> stack = new LinkedList<>();

	public Integer pop() {
		return stack.remove(stack.size() -1);
	}

	public void push(Integer elem) {
		stack.add(elem);
	}
}
