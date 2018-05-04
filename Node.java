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
    public boolean      offline;

    Node(InetAddress ip, int port, int cost) {
        this.ip           = ip;
        this.port         = port;
        this.cost         = cost;
        this.lastUpdated  = 0;
        this.offline      = false;
    }

    public String toString() {
      return address();
    }

    public String address () {
      return ip.toString() + ":" + port;
    }

    public int compareTo(Node o) {
      if (this.ip.equals(o.ip)) {
        return this.port - o.port;
      } else {
        return this.ip.toString().compareTo(o.ip.toString());
      }
    }
}
