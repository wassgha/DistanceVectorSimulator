import java.net.Inet4Address;
import java.util.*;

/*
 * Alias type for a forwarding table hash map
 */
public class ForwardingTable extends TreeMap<String, Node> {
    public ForwardingTable() {
        super();
    }
    /*
     * Converts TreeMap into nicely formatted string
     */
    public String toString() {
      String result = "";
      Iterator it = this.entrySet().iterator();
      while (it.hasNext()) {
        String key = (String)(((Map.Entry) it.next()).getKey());
        result += "\t" + key
                       + " => "
                       + this.get(key).toString()
                       + (it.hasNext() ? "\n" : "");
      }
      return result;
    }
}
