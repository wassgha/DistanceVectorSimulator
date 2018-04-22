import java.net.*;
import java.util.*;

/*
 * Abstract representation of a node (Router)
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
        this.lastUpdated  = new Date().getTime();
    }

    public String toString() {
      return "" + this.ip + ":" + this.port;
    }

    public int compareTo(Node o) {
      if (this.ip.equals(o.ip)) {
        return this.port - o.port;
      } else {
        return this.ip.toString().compareTo(o.ip.toString());
      }
    }
}
