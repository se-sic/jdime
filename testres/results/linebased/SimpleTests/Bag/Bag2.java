class Bag {
  int[] values;
<<<<<<<
  Bag(int[] v) { values = v; }
  int[] get() {
    return values;
=======
  String name;
  Bag(int[] v, String name) { values = v; this.name = name; }
  int size() {
    return values.length;
>>>>>>>
  }
}