import java.util.*;

/*
 * Alias type for a distance vector hash map
 */
public class DistanceVector extends TreeMap<String, Integer> {
    public DistanceVector() {
        super();
    }

    /*
     * Converts the distance vector to a string by key,value pairs
     *
     * @return encoded string
     */
    public String encode() {
      String result = "";
      Iterator it = this.entrySet().iterator();
      while (it.hasNext()) {
        String entryKey = (String)((Map.Entry) it.next()).getKey();
        result += "\n" + entryKey + "," + this.get(entryKey);
      }
      return result;
    }

    /*
     * Converts distance vector to nicely formatted string for logging
     *
     * @return pretty printed string
     */
    public String toString() {
      String result = "";
      Iterator it = this.entrySet().iterator();
      while (it.hasNext()) {
        String ip = (String)((Map.Entry) it.next()).getKey();
        String formattedEntry = 
            this.get(ip) == Integer.MAX_VALUE ? 
                "∞" : 
                Integer.toString(this.get(ip));
        result += formattedEntry + "\t";
      }
      return result;
    }
}
