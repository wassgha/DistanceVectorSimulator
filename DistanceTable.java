import java.util.*;

/*
 * Alias type for a distance table hash map
 */
public class DistanceTable extends TreeMap<Node, DistanceVector> {
    public DistanceTable() {
        super();
    }

    public DistanceVector put(Node n, DistanceVector dv) {
      // Add fields from existing distance vectors to current one
      Iterator it = this.entrySet().iterator();
      while (it.hasNext()) {
        Node neighbor = (Node)((Map.Entry) it.next()).getKey();
        Iterator tmp = this.get(neighbor).entrySet().iterator();
        while (tmp.hasNext()) {
          Node entry = (Node)((Map.Entry) tmp.next()).getKey();
          if (!dv.containsKey(entry)) dv.put(entry.toString(), Integer.MAX_VALUE);
        }
      }

      // Add fields from new distance vector to existing ones
      it = dv.entrySet().iterator();
      while (it.hasNext()) {
        String entry = (String)((Map.Entry) it.next()).getKey();
        Iterator tmp = this.entrySet().iterator();
        while (tmp.hasNext()) {
          Node neighbor = (Node)((Map.Entry) tmp.next()).getKey();
          if (!this.get(neighbor).containsKey(entry)) this.get(neighbor).put(entry, Integer.MAX_VALUE);
        }
      }

      return super.put(n, dv);
    }

    public String toString() {
      String result = "\t\t|\t";
      Iterator it = this.entrySet().iterator();
      while (it.hasNext()) {
        Node tmp = (Node)((Map.Entry) it.next()).getKey();
        System.out.println(tmp.toString() + " (Cost " + tmp.cost + ")\t|\t" + this.get(tmp));
      }
      return result;
    }
}
