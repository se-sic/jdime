class Bag {
  int[] values;
  Bag(int[] v) {
    super();
    values = v;
  }
  int[] get() {
    return values;
  }
  int size() {
    return values.length;
  }
}
