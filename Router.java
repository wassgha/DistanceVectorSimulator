import java.io.*;
import java.net.*;
import java.util.*;

public class Router {

    /**********************
     * Static Methods *
     **********************/

    /*
     * Validates the parameters and creates a router
     *
     * @param args command line arguments
     */
    public static void main(String args[]) throws Exception {
        if (args.length < 1 || (args.length == 1 && args[0].equals("-reverse")))
            alert("Usage: java Router [-reverse] configFile");

        boolean poisonedReverse = args[0].equals("-reverse");
        String configFile = args[0].equals("-reverse") ? args[1] : args[0];

        Router router = new Router(poisonedReverse, configFile);
    }

    /*
     * Outputs pretty exceptions
     *
     * @param e exception to print out
     */
    public static void alert(Exception e) {
        System.out.println("✖ An error occured: ");
        e.printStackTrace();
        System.exit(0);
    }

    /*
     * Outputs pretty text-based errors
     *
     * @param e error message to print out
     */
    public static void alert(String e) {
        System.out.println("✖ An error occured: ");
        System.out.println(e);
        System.exit(0);
    }

    /**********************
     * Instance Variables *
     **********************/

    private DistanceTable distanceTable;
    private ForwardingTable forwardingTable;
    public TreeMap<String, Integer> neighbors;

    private Timer timer;
    private Node thisRouter;
    private DatagramSocket socket;
    private boolean poisonedReverse;
    private long updateInterval = 10000;

    /**********************
     * Threads *
     **********************/

    /*
     * User input reading thread
     */
    class InputLoopThread extends Thread {
        InputLoopThread() {
        }

        public void run() {
            try {
                while (true) {
                    BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
                    String sentence = inFromUser.readLine();
                    String[] chunks = sentence.split(" ");
                    String message;

                    switch (chunks[0]) {
                    case "PRINT":
                        System.out.println("\n\033[0;32mDistance Vector:\033[0m");
                        System.out.println("\t" + distanceTable.get(thisRouter).toString());
                        System.out.println("\n\033[0;33mDistance Vectors Table on this router:\033[0m");
                        System.out.println(distanceTable);

                        if (forwardingTable != null) {
                            System.out.println("\n\033[0;33mForwarding Table on this router:\033[0m");
                            System.out.println(forwardingTable.toString());
                        }
                        break;
                    case "MSG":
                        if (chunks.length != 4)
                          alert("Usage: MSG <dst-ip> <dst-port> <msg>");

                        sendMessage(chunks[1], chunks[2], chunks[3]);
                        break;
                    case "CHANGE":
                        if (chunks.length != 4)
                          alert("Usage: CHANGE <dst-ip> <dst-port> <new-weight>");

                        distanceTable.change(
                            "/" + chunks[1], Integer.parseInt(chunks[2]),
                            Integer.parseInt(chunks[3])
                        );

                        DistanceVector dv = distanceTable.get(thisRouter);
                        message = "wc:" + thisRouter.address() + " " + chunks[3];
                        send(InetAddress.getByName(chunks[1]), Integer.parseInt(chunks[2]), message);
                        broadcast("dv:" + thisRouter.address() + dv.encode());
                        break;
                    case "BROADCAST":
                        if (chunks.length != 2)
                          alert("Usage: BROADCAST <msg>");

                        message = "bc: " + chunks[1] + " " + thisRouter.address();
                        broadcast(message, true);
                        break;
                    default:
                        break;
                    }

                    log();

                }
            } catch (Exception e) {
                alert(e);
            }
        }
    }

    /*
     * Thread that listens for updates and incoming messages
     */
    class ListenerThread extends Thread {
        ListenerThread() {
        }

        public void run() {
            try {
                log("\uD83C\uDF0D Listening on port " + thisRouter.port);

                while (true) {
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    String data = new String(receivePacket.getData());


                    switch (data.substring(0, 3)) {
                        case "dv:":
                            synchronized (distanceTable) {
                                distanceTable.update(data.substring(3));
                                log("Received:\n" + data + "\n");

                                forwardingTable = distanceTable.calculate(thisRouter);
                                log("⟳ Updated distance table");
                                log(distanceTable.toString());
                                log("⟳ Updated forwarding table");
                                log(forwardingTable.toString());
                            }
                            break;
                        case "fd:":
                            String address = data.substring(3, data.indexOf('\n'));
                            String[] chunks = address.split(":");
                            log("⇅ RECEIVED DATA");
                            log("Address -> " + address);
                            log(data);
                            sendMessage(
                                chunks[0],
                                chunks[1],
                                data.substring(data.indexOf('\n') + 1)
                            );
                            break;
                        case "wc:":
                            synchronized (distanceTable) {
                                String[] tmp = data.substring(3).trim().split(" ");
                                String[] details = tmp[0].split(":");

                                log(tmp[0] + " " + tmp[1]);

                                distanceTable.change(
                                    details[0], Integer.parseInt(details[1]),
                                    Integer.parseInt(tmp[1])
                                );

                                DistanceVector dv = distanceTable.get(thisRouter);
                                broadcast("dv:" + thisRouter.address() + dv.encode());

                                log(distanceTable);
                            }
                            break;
                          case "bc:":
                            synchronized (distanceTable) {
                              log("⇅ RECEIVED BROADCAST : " + data.substring(3).trim());
                              broadcast(
                                  data.trim() + " " + thisRouter.address(),
                                  true
                              );
                              break;
                            }
                        default:
                            log("⇅ RECEIVED DATA");
                            log(data);
                            break;
                    }



                }
            } catch (Exception e) {
                alert(e);
            }
        }
    }

    /*
     * Thread that runs periodically and sends updates to the node's neighbors
     */
    public class TimedUpdateThread extends TimerTask {
        @Override
        public void run() {
            log("➠ Broadcasting periodical updates... ");
            synchronized (distanceTable) {
                DistanceVector dv = distanceTable.get(thisRouter);

                Set keys = distanceTable.keySet();
                Node tmp = null;
                Iterator i = keys.iterator();
                ArrayList<Node> keysToBeRemoved = new ArrayList<Node>();


                while (i.hasNext()) {
                    Node key = (Node) i.next();
                    // if (tmp == null) continue;

                    if (!key.address().equals(thisRouter.address())) key.lastUpdated++;
                    System.out.println("LAST UPDATED " + key.address() + " " + key.lastUpdated);
                    if (key.lastUpdated >= 3) {
                        keysToBeRemoved.add(key);
                    }
                }

                for (int index = 0; index < keysToBeRemoved.size(); index++) {
                    distanceTable.remove(keysToBeRemoved.get(index));
                    distanceTable.removeColumn(keysToBeRemoved.get(index).address());
                }

                forwardingTable = distanceTable.calculate(thisRouter);

                broadcast("dv:" + thisRouter.address() + dv.encode());
            }
        }
    }

    /**********************
     * Main Methods *
     **********************/

    /*
     * Constructor - Creates a router, initializes it and sets up routines for
     * reading input, sending periodical updates and listening for messages
     *
     * @param poisonedReverse whether to use poisoned reverse or not
     *
     * @param configFile router's neighbors definition (path to a file)
     */
    public Router(boolean poisonedReverse, String configFile) {
        this.poisonedReverse = poisonedReverse;

        initRouter(configFile);

        log("⚐ Initial Distance Table");
        log(this.distanceTable.toString());

        ListenerThread listener = new ListenerThread();
        listener.start();

        InputLoopThread inputLoop = new InputLoopThread();
        inputLoop.start();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(
            new TimedUpdateThread(),
            updateInterval,
            updateInterval
        );
    }

    /*
     * initRouter - Configures and sets up the current router based on a given
     * configuration file. Also initializes the default distance table.
     *
     * @param configFile path to the configuration file
     */
    public void initRouter(String configFile) {
        log("⌛ Reading neighbor nodes...");

        // Parse configuration file
        try {
            File file = new File(configFile);
            Scanner input = new Scanner(file);

            // Read this router's configuration
            InetAddress thisIp = InetAddress.getByName(input.next());
            int thisPort = input.nextInt();
            this.thisRouter = new Node(thisIp, thisPort, 0);

            // Initialize distance table and neighbor list
            this.distanceTable = new DistanceTable(this);
            this.forwardingTable = new ForwardingTable();
            this.neighbors = new TreeMap<String, Integer>();

            // Read neighbors from file and store them in the initial dv
            DistanceVector dv = new DistanceVector();
            // Add self as node in DV
            dv.put(thisIp + ":" + thisPort, 0);

            while (input.hasNext()) {
                InetAddress ip = InetAddress.getByName(input.next());
                int port = input.nextInt();
                int cost = input.nextInt();

                Node neighbor = new Node(ip, port, cost);
                neighbors.put(neighbor.address(), cost);

                // Add the neighbor to the distance vector (as a column on the
                // current router's row of the distance table)
                dv.put(neighbor.toString(), cost);
                // Also add node to distance table
                // Since the rows of the table are neighbors
                this.distanceTable.put(neighbor, new DistanceVector());
                this.forwardingTable.put(ip.toString() + ":" + port, neighbor);

                log("✓ Added node: IP (" + ip + ") , Port (" + port + "), Cost " + cost);
            }

            // After adding all the neighbors to the dv, adds it to the distance table
            this.distanceTable.put(thisRouter, dv);

            input.close();
        } catch (Exception e) {
            alert(e);
        }

        log("✓ Finished constructing initial distance table");

        // Initialize the UDP Connection
        try {
            this.socket = new DatagramSocket(thisRouter.port);
        } catch (Exception e) {
            alert(e);
        }

        log("\uD83D\uDCE1 Router deployed!");
    }

    public void broadcast(String message) {
      broadcast(message, false);
    }

    public void broadcast(String message, boolean reversePathForward) {
        try {
            // Get all neighboring nodes from the distance table
            Iterator it = this.distanceTable.entrySet().iterator();

            // Contains all the IP addresses the message was broadcasted to in
            // case of a bc: message
            String[] broadcastIps = new String[0];

            DistanceVector dv = null;
            if (message.substring(0, 3).equals("dv:")) {
                dv = distanceTable.get(thisRouter);
            }

            // If reverse path forwarding is on then only broadcast if the
            // node came on the shortest path from its source
            if (message.substring(0, 3).equals("bc:") && reversePathForward) {
              broadcastIps = message.substring(3).trim().split(" ");
              broadcastIps =  Arrays.copyOfRange(broadcastIps, 1, broadcastIps.length);

              String lastAddress = broadcastIps[broadcastIps.length - 1];
              String sourceAddress = broadcastIps.length >= 1 ? broadcastIps[0] : lastAddress;

              if (this.forwardingTable.containsKey(sourceAddress) &&
                  !this.forwardingTable.get(sourceAddress).address().equals(lastAddress)) {
                    log("✖ Broadcast message rejected (reverse path forwarding)");
                    return;
              }
            }

            // Broadcast the message to all neighboring nodes
            broadcasting: while (it.hasNext()) {
                Node neighbor = (Node) ((Map.Entry) it.next()).getKey();
                // Don't broadcast to self
                if (neighbor.address().equals(thisRouter.address()))
                    continue;

                // Do not broadcast to nodes that already received the message
                if (message.substring(0, 3).equals("bc:")
                    && Arrays.asList(broadcastIps).contains(neighbor.address()))
                    continue broadcasting;

                // poison reverse
                if (dv != null && this.poisonedReverse) {
                   message = poison(neighbor, dv);
                }

                send(neighbor.ip, neighbor.port, message);

                if (message.substring(0, 3).equals("bc:"))
                  log("➠ Broadcast message forwarded to " + neighbor.port);
            }
        } catch (Exception e) {
            alert(e);
        }
    }

    public String poison (Node neighbor, DistanceVector dv) {
        DistanceVector poisonedDv = new DistanceVector();
        Set<String> keys = dv.keySet();
        String message = "";

        for (String key: keys) {

            if (
                forwardingTable.containsKey(key) &&
                forwardingTable.get(key).compareTo(neighbor) == 0 &&
                !key.equals(forwardingTable.get(key).address())
            ) {
                poisonedDv.put(key, Integer.MAX_VALUE);
            } else {
                poisonedDv.put(key, dv.get(key));
            }
        }
        message = "dv:" + thisRouter.address() + poisonedDv.encode();

        return message;
    }

    public void sendMessage (String ip, String port, String message) {
        try {
            Node nextHop = this.forwardingTable.get("/" + ip + ":" + port);

            if (nextHop == null) {
                System.out.println("No entry in forwarding table -> Dropping");
                return;
            }

            log("NextHop -> " + nextHop.ip.toString() + "\tIp -> " + ip);

            if (!nextHop.ip.toString().equals("/" + ip)
                || nextHop.port != Integer.parseInt(port))
                message = "fd:"
                            + ip + ":" + port + '\n'
                            + message + " "
                            + nextHop.ip.getHostAddress() + ":" + nextHop.port;

            log("Sending: " + message);
            send(nextHop.ip, nextHop.port, message);
        } catch (Exception e) {
            alert(e);
        }
    }

    public void send(InetAddress ip, int port, String message) {
        try {
            DatagramPacket sendPacket = new DatagramPacket(
                message.getBytes(),
                message.getBytes().length,
                ip,
                port
            );

            socket.send(sendPacket);
        } catch (Exception e) {
            alert(e);
        }
    }

    public Node node () {
        return thisRouter;
    }

    public ForwardingTable forwardingTable () {
        return forwardingTable;
    }

    public void log(Object str) {
        System.out.println(str.toString());
    }

    public void log(String str) {
        System.out.println(str);
    }

    public void log() {
        System.out.println();
    }
}
