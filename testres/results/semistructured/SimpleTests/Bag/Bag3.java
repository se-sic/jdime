
class Bag {
  int[] values;

  String getString() {
    String res = "";
<<<<<<<
    String sep = ",";
=======
    String sep = ";";
>>>>>>>
    for(int v : values) {
      res += v + sep;
    }
    return res;
  }

  Bag(int[] v) {
     values = v; 
  }
}