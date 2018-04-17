import java.io.*;
import java.net.*;
import java.util.*;

public class Router
{
    // Static methods
    
    public static void main(String args[]) throws Exception
    {
        if (args.length < 1 || (args.length == 1 && args[0].equals("-reverse")))
            alert("Usage: java Router [-reverse] configFile");

        boolean poisonedReverse = args[0].equals("-reverse");
        String configFile = args[0].equals("-reverse") ? args[1] : args[0];
        Router router = new Router(poisonedReverse, configFile);
    }
    
    public static void alert(Exception e) {
        System.out.println("✖ Error occured: ");
        e.printStackTrace();
        System.exit(0);
    }
    
    public static void alert(String e) {
        System.out.print("✖ Error occured: ");
        System.out.println(e);
        System.exit(0);
    }
    
    // Instance variables
    
    private boolean poisonedReverse;
    private HashMap<Node, DistanceVector> distanceTable;
    private ArrayList<Node> neighbors;
    private Timer timer;
    private Node thisRouter;
    private DatagramSocket socket;
    
    private long updateInterval = 2000;
    
    // Child classes
    
    public class Node {
        public InetAddress ip;
        public int port;
        public long lastUpdated;
        
        Node(InetAddress ip, int port) {
            this.ip = ip;
            this.port = port;
            this.lastUpdated = new Date().getTime();
        }
    }
    
    public class DistanceVector extends HashMap<Node, Integer> {
        public DistanceVector() {
            super();
        }
    }

    
    public class TimedUpdate extends TimerTask {
      @Override
      public void run() {
        System.out.println("➠ Broadcasting updates... ");
      }
    }
    
    class InputLoopThread extends Thread {
        InputLoopThread() {
        }
        
        public void run() {
            try {
                while(true)
                {
                    BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
                    byte[] sendData = new byte[1024];
                    String sentence = inFromUser.readLine();
                    sendData = sentence.getBytes();
                    
                    DistanceVector dv = distanceTable.get(thisRouter);
                    Iterator it = dv.entrySet().iterator();
                    
                    while (it.hasNext()) {
                        Node neighbor = (Node)((Map.Entry) it.next()).getKey();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, neighbor.ip, neighbor.port);
                        socket.send(sendPacket);
                    }
                }
            } catch (Exception e)  {
                Router.alert(e);
            }
        }
    }

    
    class ListenerThread extends Thread {
        ListenerThread() {
        }
        
        public void run() {
            try {
                System.out.println("\uD83C\uDF0D Listening on port " + thisRouter.port);
                byte[] receiveData = new byte[1024];
                while(true)
                {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    String sentence = new String(receivePacket.getData());
                    // InetAddress IPAddress = receivePacket.getAddress();
                    // int rcvPort = receivePacket.getPort();
                    System.out.println("Received data " + sentence);
                }
            } catch (Exception e) {
                Router.alert(e);
            }
        }
    }
    
    // Instance methods
    
    public Router(boolean poisonedReverse, String configFile)
    {
        this.poisonedReverse = poisonedReverse;
        
        initRouter(configFile);
        
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimedUpdate(), 0, updateInterval);
        
        ListenerThread listener = new ListenerThread();
        listener.start();
        
        InputLoopThread inputLoop = new InputLoopThread();
        inputLoop.start();
    }
    
    public String buildMsg(int type, String content) {
        return "";
    }
    
    public void sendMsg() {
    }
    
    public void inputThread() {
    }

    public void initRouter(String configFile)
    {
        this.neighbors = new ArrayList<Node>();
        System.out.println("⌛ Reading neighbor nodes...");
        
        // Parse configuration file
        try {
            File file = new File(configFile);
            Scanner input = new Scanner(file);
            
            // Read this router's configuration
            InetAddress thisIp = InetAddress.getByName(input.next());
            int thisPort = input.nextInt();
            this.thisRouter = new Node(thisIp, thisPort);
            
            // Initialize distance table
            this.distanceTable = new HashMap<Node, DistanceVector>();
            
            // Read neighbors from file and store them in the initial dv
            DistanceVector dv = new DistanceVector();
            while(input.hasNext()) {
                InetAddress ip = InetAddress.getByName(input.next());
                int port = input.nextInt();
                int cost = input.nextInt();
                
                Node neighbor = new Node(ip, port); 
                this.neighbors.add(neighbor);
                
                dv.put(neighbor, cost);
                this.distanceTable.put(neighbor, null);
            }
            this.distanceTable.put(thisRouter, dv);
            
            input.close();
        } catch (Exception e) {
            alert(e);
        }

        
        System.out.println("✓ Added node: IP , Port , Cost ");
        
        try {
            this.socket = new DatagramSocket(thisRouter.port);
        } catch (Exception e) {
            alert(e);
        }
        
        System.out.println("\uD83D\uDCE1 Router created!");
    }
    
}
