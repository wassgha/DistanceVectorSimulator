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

        boolean logger = true;
        if ((args.length == 2 && !args[0].equals("-reverse")) || args.length > 2) {
            logger = args[0].equals("-reverse") ? args[2].equals("true") : args[1].equals("true");
        }

        Router router = new Router(poisonedReverse, configFile, logger);
    }

    /*
     * Outputs pretty exceptions
     *
     * @param e exception to print out
     */
    public static void alert(Exception e) {
        System.out.print("✖ An error occured: ");
        e.printStackTrace();
        System.exit(0);
    }

    /*
     * Outputs pretty text-based errors
     *
     * @param e error message to print out
     */
    public static void alert(String e) {
        System.out.print("✖ An error occured: ");
        System.out.print(e);
        System.exit(0);
    }

    /**********************
     * Instance Variables *
     **********************/

    private DistanceTable distanceTable;
    private TreeMap<String, Node> forwardingTable;

    private Timer timer;
    private Node thisRouter;
    private DatagramSocket socket;
    private boolean poisonedReverse;
    private long updateInterval = 10000;
    private boolean logger = true;

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
                        synchronized (distanceTable) {
                            sendMessage(chunks[1], chunks[2], chunks[3]);
                        }
                        break;
                    case "CHANGE":

                        distanceTable.updateSelf(
                            "/" + chunks[1], Integer.parseInt(chunks[2]), 
                            Integer.parseInt(chunks[3])
                        );

                        DistanceVector dv = distanceTable.get(thisRouter);
                        String message = "dv:" + thisRouter.toString() + dv.encode();
                        send(InetAddress.getByName(chunks[1]), Integer.parseInt(chunks[2]), message);
                        break;
                    default:
                        break;
                    }

                    System.out.println();

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


                                forwardingTable = distanceTable.calculate();
                                log("⟳ Updated distance table");
                                log(distanceTable.toString());
                                log("⟳ Updated forwarding table");
                                log(forwardingTable.toString());
                            }
                            break;
                        case "fd:":
                            String address = data.substring(3, data.indexOf('\n'));
                            String[] chunks = address.split(":");
                            System.out.println("⇅ RECEIVED DATA");
                            System.out.println("Address -> " + address);
                            System.out.println(data);
                            sendMessage(chunks[0], chunks[1], data.substring(data.indexOf('\n') + 1));
                            break;
                        default:
                            System.out.println("⇅ RECEIVED DATA");
                            System.out.println(data);
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
                // log("Broadcast " + dv.encode() + "END" + thisRouter.toString());
                broadcast("dv:" + thisRouter.toString() + dv.encode());
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
    public Router(boolean poisonedReverse, String configFile, boolean logger) {
        this.poisonedReverse = poisonedReverse;
        this.logger = logger;

        initRouter(configFile);

        System.out.println("⚐ Initial Distance Table");
        System.out.println(this.distanceTable.toString());

        ListenerThread listener = new ListenerThread();
        listener.start();

        InputLoopThread inputLoop = new InputLoopThread();
        inputLoop.start();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimedUpdateThread(), updateInterval, updateInterval);
    }

    /*
     * initRouter - Configures and sets up the current router based on a given
     * configuration file. Also initializes the default distance table.
     *
     * @param configFile path to the configuration file
     */
    public void initRouter(String configFile) {
        System.out.println("⌛ Reading neighbor nodes...");

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
            this.forwardingTable = new TreeMap<String, Node>();

            // Read neighbors from file and store them in the initial dv
            DistanceVector dv = new DistanceVector();
            // Add self as node in DV
            dv.put(thisIp + ":" + thisPort, 0);

            while (input.hasNext()) {
                InetAddress ip = InetAddress.getByName(input.next());
                int port = input.nextInt();
                int cost = input.nextInt();

                Node neighbor = new Node(ip, port, cost);

                // Add the neighbor to the distance vector (as a column on the
                // current router's row of the distance table)
                dv.put(neighbor.toString(), cost);
                // Also add node to distance table
                // Since the rows of the table are neighbors
                this.distanceTable.put(neighbor, new DistanceVector());
                this.forwardingTable.put(ip.toString() + ":" + port, neighbor);

                System.out.println("✓ Added node: IP (" + ip + ") , Port (" + port + "), Cost " + cost);
            }

            // After adding all the neighbors to the dv, adds it to the distance table
            this.distanceTable.put(thisRouter, dv);

            input.close();
        } catch (Exception e) {
            alert(e);
        }

        System.out.println("✓ Finished constructing initial distance table");

        // Initialize the UDP Connection
        try {
            this.socket = new DatagramSocket(thisRouter.port);
        } catch (Exception e) {
            alert(e);
        }

        System.out.println("\uD83D\uDCE1 Router deployed!");
    }

    public void broadcast(String message) {
        try {
            // Get all neighboring nodes from the distance table
            Iterator it = this.distanceTable.entrySet().iterator();

            // Broadcast the message to all neighboring nodes
            while (it.hasNext()) {
                Node neighbor = (Node) ((Map.Entry) it.next()).getKey();
                // Don't broadcast to self
                if (neighbor == thisRouter)
                    continue;
                send(neighbor.ip, neighbor.port, message);
            }
        } catch (Exception e) {
            alert(e);
        }
    }

    public void sendMessage (String ip, String port, String message) {
        try {
            Node nextHop = this.forwardingTable.get("/" + ip + ":" + port);
            System.out.println("NextHop -> " + nextHop.ip.toString() + "\tIp -> " + ip);

            if (!nextHop.ip.toString().equals("/" + ip) || nextHop.port != Integer.parseInt(port)) 
                message = "fd:" + ip + ":" + port + '\n' + message + " " + nextHop.ip.getHostAddress() + ":" + nextHop.port;

            System.out.println("Sending: " + message);
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

    public TreeMap<String, Node> forwardingTable () {
        return forwardingTable;
    }

    public void log(String str) {
        if (this.logger) {
            System.out.println(str);
        }
    }
}
