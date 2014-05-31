class Bag {
  int[] values;
  Bag(int[] v) { values = v; }
  String getString() {
    String res = "";
    String sep = ",";
    for(int v : values) {
      res += v + sep;
    }
    return res;
  }
}
