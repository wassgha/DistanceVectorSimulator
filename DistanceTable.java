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
          String entry = (String)((Map.Entry) tmp.next()).getKey();
          if (!dv.containsKey(entry)) dv.put(entry, Integer.MAX_VALUE);
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

    public void update(String data) {
      String[] lines = data.split("\n");
      if (lines.length == 0) return;

      String[] firstLine = lines[0].split(":");
      Node targetNode = getNode(firstLine[0], firstLine[1]);
      DistanceVector dv = this.get(targetNode);
      if (dv == null) return;

      DistanceVector newDV = new DistanceVector();
      for (int i = 1; i < lines.length; i++) {
        String[] line = lines[i].split(",");
        String entry = line[0];
        Integer cost = Integer.parseInt(line[1].trim());
        newDV.put(entry, cost);
      }
      this.put(targetNode, newDV);
    }

    public void calculate(Node router) {
      DistanceVector dv = new DistanceVector();
      dv.put(router.toString(), 0);

      Iterator it = this.entrySet().iterator();
      while (it.hasNext()) {
        Node neighbor = (Node)((Map.Entry) it.next()).getKey();

        Iterator tmp = this.get(neighbor).entrySet().iterator();
        while (tmp.hasNext()) {
          String entry = (String)((Map.Entry) tmp.next()).getKey();
          // Don't add neighbor cost if value is infinity (causes overflow that messes up min calculation)
          int distance = this.get(neighbor).get(entry);
          if (distance != Integer.MAX_VALUE) distance += neighbor.cost;

          if (!entry.equals(router.toString()) && (dv.get(entry) == null || distance < dv.get(entry))) {
            dv.put(entry, distance);
          }
        }
      }
      this.put(router, dv);
    }

    public Node getNode(String ip, String port) {
      Iterator it = this.entrySet().iterator();
      while (it.hasNext()) {
        Node tmp = (Node)((Map.Entry) it.next()).getKey();
        if (tmp.ip.toString().equals(ip) && ("" + tmp.port).equals(port)) return tmp;
      }
      return null;
    }

    public String toString() {
      String result = "";
      Iterator it = this.entrySet().iterator();
      while (it.hasNext()) {
        Node tmp = (Node)((Map.Entry) it.next()).getKey();
        result += tmp.toString() + " (Cost " + tmp.cost + ")\t|\t" + this.get(tmp) + "\n";
      }
      return result;
    }
}
