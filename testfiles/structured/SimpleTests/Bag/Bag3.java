class Bag {
  int[] values;
  String getString() {
    String res = "";
    String sep = 
<<<<<<< testfiles/left/SimpleTests/Bag/Bag3.java
","
=======
";"
>>>>>>> testfiles/right/SimpleTests/Bag/Bag3.java
;
    for (int v : values) {
      res += v + sep;
    }
    return res;
  }
  Bag(int[] v) {
    values = v;
  }
}
