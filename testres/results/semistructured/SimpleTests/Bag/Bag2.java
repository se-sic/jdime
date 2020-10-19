
class Bag {
  int[] values;

  String name;

  Bag(int[] v, String name) {
     values = v; this.name = name; 
  }

  int[] get() {
    return values;
  }

  int size() {
    return values.length;
  }
}