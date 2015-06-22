import java.util.List;
import java.util.ArrayList;

class Stack {
	List<Integer> stack = new ArrayList<>();

	public Integer pop() {
		return stack.remove(stack.size() -1);
	}

	public void push(Integer elem) {
		stack.add(elem);
	}
}
