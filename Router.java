import java.io.*;
import java.net.*;
import java.util.*;

public class Router
{
    private boolean poisonedReverse;
    private HashMap<Node, HashMap<Node, Integer>> dv;
    private Timer timer;
    private int port;
    private DatagramSocket socket;
    
    private long updateInterval = 2000;
    
    public class Node {
        public String ip;
        public Integer port;
        public int lastUpdated;
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
                    InetAddress IPAddress = InetAddress.getByName("localhost");
                    byte[] sendData = new byte[1024];
                    String sentence = inFromUser.readLine();
                    sendData = sentence.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port + 1);
                    socket.send(sendPacket);
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
                System.out.println("\uD83C\uDF0D Listening on port " + port);
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
    
    public static void main(String args[]) throws Exception
    {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 3000;
        Router router = new Router(port);
    }
    
    public static void alert(Exception e) {
        System.out.println("✖ Error occured: ");
        e.printStackTrace();
    }
    
    public Router(int port)
    {
        this.port = port;
        
        try {
            this.socket = new DatagramSocket(port);
        } catch (Exception e) {
            alert(e);
        }
        
        System.out.println("\uD83D\uDCE1 Router created!");
        
        initNeighbors("neighbors.txt");
        
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

    public void initNeighbors(String file)
    {
        System.out.println("⌛ Reading neighbor nodes...");
        // Read neighbors from file and store them in the initial dv
        System.out.println("✓ Added node: IP , Port , Cost ");
        return;
    }
    
}
