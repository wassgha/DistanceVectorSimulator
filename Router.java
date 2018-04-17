import java.io.*;
import java.net.*;
import java.util.*;

public class Router
{
    public class Node {
        public String ip;
        public Integer port;
    }
    
    public class TimedUpdate extends TimerTask {
      @Override
      public void run() {
        System.out.println("➠ Broadcasting updates... ");
      }
    }
    
    class InputLoopThread extends Thread {
        int port = 3000;
        
        InputLoopThread() {
    
        }
        
        public void run() {
            try {
                System.out.println("\uD83C\uDF0D Listening on port " + port);
                while(true)
                {
                    BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
                    DatagramSocket clientSocket = new DatagramSocket(port);
                    InetAddress IPAddress = InetAddress.getByName("localhost");
                    byte[] sendData = new byte[1024];
                    byte[] receiveData = new byte[1024];
                    String sentence = inFromUser.readLine();
                    sendData = sentence.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
                    clientSocket.send(sendPacket);
                    DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
                    clientSocket.receive(receivePacket);
                    String modifiedSentence = new String(receivePacket.getData());
                    System.out.println("⇋ FROM SERVER:" + modifiedSentence);
                    clientSocket.close();
                }
            } catch (Exception e)  {
                Router.alert(e);
            }
        }
    }

    
    class ListenerThread extends Thread {
        int port = 9876;
        
        ListenerThread() {
    
        }
        
        public void run() {
            try {
                System.out.println("\uD83C\uDF0D Listening on port " + port);
                DatagramSocket serverSocket = new DatagramSocket(port);
                byte[] receiveData = new byte[1024];
                byte[] sendData = new byte[1024];
                while(true)
                {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);
                    String sentence = new String(receivePacket.getData());
                    InetAddress IPAddress = receivePacket.getAddress();
                    int rcvPort = receivePacket.getPort();
                    String capitalizedSentence = sentence.toUpperCase();
                    sendData = capitalizedSentence.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, rcvPort);
                    serverSocket.send(sendPacket);
                    System.out.println("Received data " + sentence);
                }
            } catch (Exception e) {
                Router.alert(e);
            }
        }
    }

    // instance variables
    private boolean poisonedReverse;
    private HashMap<Node, HashMap<Node, Integer>> dv;
    private Timer timer;
    
    private long updateInterval = 2000;
    
    
    public static void main(String args[]) throws Exception
    {
        Router router = new Router();
    }
    
    public static void alert(Exception e) {
        System.out.println("✖ Error occured: ");
        e.printStackTrace();
    }
    
    public Router()
    {
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
