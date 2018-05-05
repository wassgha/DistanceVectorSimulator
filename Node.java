import java.net.*;
import java.util.*;

/*
 * Abstract representation of a node (Router)
 * Contains the address (ip : port), cost, and last 
 * time it was updated
 */
public class Node implements Comparable<Node> {
    public InetAddress  ip;
    public int          port;
    public int          cost;
    public long         lastUpdated;

    Node(InetAddress ip, int port, int cost) {
        this.ip           = ip;
        this.port         = port;
        this.cost         = cost;
        this.lastUpdated  = 0;
    }

    public String toString() {
      return address();
    }

    /**
     * Get the address of the node
     * @return address - ip and port combined
     */
    public String address () {
      return ip.toString() + ":" + port;
    }

    /**
     * Comare the node to another one based on its address
     * @return int - 0 if equal
     */
    public int compareTo(Node o) {
      if (this.ip.equals(o.ip)) {
        return this.port - o.port;
      } else {
        return this.ip.toString().compareTo(o.ip.toString());
      }
    }
}
