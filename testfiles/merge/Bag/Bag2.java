class Bag {
  int[] values;
  String name;
  Bag(int[] v, String name) {
    super();
    values = v;
    this.name = name;
  }
  int[] get() {
    return values;
  }
  int size() {
    return values.length;
  }
}