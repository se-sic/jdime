import java.util.List;
class Bar {
	List<String> l;
	int bar() {
		String s = l.get(0);
		return s.length();
	}
}
