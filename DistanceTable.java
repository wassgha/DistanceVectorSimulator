import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.*;

/*
 * Alias type for a distance table hash map
 */
public class DistanceTable extends TreeMap<Node, DistanceVector> {
    private Router router;

    public DistanceTable(Router router) {
        super();
        // Router where this distance table exists
        this.router = router;
    }

    /*
     * Extends default put method to ensure all DistanceVectors
     *  contain the same columns in order
     *
     * @param n node whose DV to update
     * @param dv distance vector to be added
     */
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

    /*
     * Takes string from socket, decodes it, and updates just the
     *  row that was received
     *
     * @param data string encoded distance vector received from socket
     */
    public void update(String data) throws Exception {
      String[] lines = data.split("\n");
      if (lines.length == 0) return;

      // Identifies sender of data
      String[] firstLine = lines[0].split(":");
      Node targetNode = getNode(firstLine[0], firstLine[1]);
      boolean oldNeighbor = targetNode == null;

      // check if this is distance vector of a neighbor 
      // that went offline and is back online
      // if so create new node and add it among neighbors
      // otherwise check that distance vector for that 
      // node exists, if not just ignore it, since it is not
      // neighbor
      if (oldNeighbor) {
        targetNode = new Node(
          InetAddress.getByName(firstLine[0].substring(1)),
          Integer.parseInt(firstLine[1]),
          router.neighbors.get(firstLine[0] + ":" + firstLine[1])
        );
      }
      else {
        DistanceVector dv = this.get(targetNode);
        if (dv == null) return;
      }

      // Adds each entry in encoded string to DV object
      DistanceVector newDV = new DistanceVector();
      for (int i = 1; i < lines.length; i++) {
        String[] line = lines[i].split(",");
        String entry = line[0];
        Integer cost = Integer.parseInt(line[1].trim());
        // If there is no column for that node, and this node is not old neighbor just continue the loop
        // this statement is used when node goes offlin and comes back online after some time, and other nodes
        // have to reinsert it back and increase number of columns
        if (!this.get(router.node()).containsKey(entry) && router.neighbors.containsKey(entry) && !oldNeighbor) 
          continue;
        newDV.put(entry, cost);

      }

      // reset the lastUpdated property
      targetNode.lastUpdated = 0;
      this.put(targetNode, newDV);
    }

    public void change(String ip, int port, int cost) {
      getNode(ip + ":" + port).cost = cost;
    }


    /*
     * Updates distance vector for given node
     *
     * @param router node whose distance vector to update
     * @return new forwarding table for this node
     */
    public ForwardingTable calculate(Node router) {

      DistanceVector dv = new DistanceVector();
      dv.put(router.toString(), 0);
      ForwardingTable forwardingTable = new ForwardingTable();

      Iterator it = this.entrySet().iterator();
      while (it.hasNext()) {
        Node neighbor = (Node)((Map.Entry) it.next()).getKey();
        if (neighbor == router) continue;

        Iterator tmp = this.get(neighbor).entrySet().iterator();
        while (tmp.hasNext()) {
          String entry = (String)((Map.Entry) tmp.next()).getKey();
          // Don't add neighbor cost if value is infinity (causes overflow that messes up min calculation)
          int distance = this.get(neighbor).get(entry);
          if (distance != Integer.MAX_VALUE) distance += neighbor.cost;

          if (!entry.equals(router.toString()) && (dv.get(entry) == null || distance < dv.get(entry))) {
            dv.put(entry, distance);
            forwardingTable.put(entry, neighbor);
          }
        }
      }
      this.put(router, dv);
      return forwardingTable;
    }

    /*
     * Finds the node in the table given its IP and port
     *
     * @param ip string version of INetAddress
     * @param port string version of port #
     * @return Node object for row in table
     */
    public Node getNode(String ip, String port) {
      Iterator it = this.entrySet().iterator();
      while (it.hasNext()) {
        Node tmp = (Node)((Map.Entry) it.next()).getKey();
        if (tmp.ip.toString().equals(ip) && ("" + tmp.port).equals(port)) return tmp;
      }
      return null;
    }

     /*
     * Finds the node in the table given its address
     *
     * @param address - ip + ":" + port
     * @return Node object for row in table
     */
    public Node getNode(String address) {
     String[] chunks = address.split(":");
     return getNode(chunks[0], chunks[1]);
    }


    /**
     * Remove column from distance table
     * used when one of the nodes goes offline and
     * the others have to drop it.
     * 
     * @param key - String
     */
    public void removeColumn (String key) {
      System.out.println("Removing column " + key);
      Set<Node> nodes = this.keySet();
      DistanceVector dv = null;

      for (Node node : nodes) {
        dv = this.get(node);
        dv.remove(key);
      }
    }

    /*
     * Converts TreeMap into nicely formatted string
     */
    public String toString() {
      String result = "";
      Iterator it = this.entrySet().iterator();
      while (it.hasNext()) {
        Node node = (Node)((Map.Entry) it.next()).getKey();
        result += "\t" + node.toString()
                       + " (Cost " + node.cost + ")\t|\t"
                       + this.get(node)
                       + (it.hasNext() ? "\n" : "");
      }
      return result;
    }
}
