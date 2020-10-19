class Bag {
  String getString() {
    String res = "";
    String sep = ";";
    for(int v : values) {
      res += v + sep;
    }
    return res;
  }
  int[] values;
  Bag(int[] v) { values = v; }
}
