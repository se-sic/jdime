import java.util.List;
class Bar {
	List<String> l;
	int bar() {
		if (l != null) {
			String s = l.get(0);
			return s.length();
		}
		return 0;
	}
}
