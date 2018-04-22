import java.util.*;

/*
 * Alias type for a distance vector hash map
 */
public class DistanceVector extends TreeMap<String, Integer> {
    public DistanceVector() {
        super();
    }

    public String toString() {
      String result = "";
      Iterator it = this.entrySet().iterator();
      // while (it.hasNext()) {
      //   Node tmp = (Node)((Map.Entry) it.next()).getKey();
      //   result += tmp.ip + ":" + tmp.port + " - " + this.get(tmp) + "\n";
      // }
      while (it.hasNext()) {
        String tmp = (String)((Map.Entry) it.next()).getKey();
        result += this.get(tmp) + "\t";
      }
      return result;
    }
}
