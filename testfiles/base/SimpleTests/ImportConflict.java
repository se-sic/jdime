import java.util.List;

class Stack {
	List<Integer> stack;

	public Integer pop() {
		return stack.remove(stack.size() -1);
	}

	public void push(Integer elem) {
		stack.add(elem);
	}
}
