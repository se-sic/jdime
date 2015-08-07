class Bag {
  int[] values;
<<<<<<< testfiles/left/SimpleTests/Bag/Bag2.java
  Bag(int[] v) { values = v; }
  int[] get() {
    return values;
=======
  String name;
    Bag(int[] v, String name) { values = v; this.name = name; }
  int size() {
    return values.length;
>>>>>>> testfiles/right/SimpleTests/Bag/Bag2.java
  }
}
