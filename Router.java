import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Write a description of class Router here.
 *
 * @author (your name)
 * @version (a version number or a date)
 */
public class Router
{
    public class Node {
        public String ip;
        public Integer port;
    }
    
    public class TimedUpdate extends TimerTask {
      @Override
      public void run() {
        System.out.println("Broadcasted updates... ");
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
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName("hostname");
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];
        String sentence = inFromUser.readLine();
        sendData = sentence.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
        clientSocket.send(sendPacket);
        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
        clientSocket.receive(receivePacket);
        String modifiedSentence = new String(receivePacket.getData());
        System.out.println("FROM SERVER:" + modifiedSentence);
        clientSocket.close();
    }
    
    /**
     * Constructor for objects of class Router
     */
    public Router()
    {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimedUpdate(), 0, updateInterval);
        
        initNeighbors("neighbors.txt");
    }
    
    public String buildMsg(int type, String content) {
        return "";
    }
    
    public void sendMsg() {
    }
    
    public void inputThread() {
    }
    
    public void listenerThread() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(9876);
            byte[] receiveData = new byte[1024];
            byte[] sendData = new byte[1024];
            while(true)
            {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                String sentence = new String(receivePacket.getData());
                InetAddress IPAddress = receivePacket.getAddress();
                int port = receivePacket.getPort();
                String capitalizedSentence = sentence.toUpperCase();
                sendData = capitalizedSentence.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData,
                sendData.length, IPAddress, port);
                serverSocket.send(sendPacket);
            }
        } catch (Exception e) {
            System.out.println("Error occured: ");
            e.printStackTrace();
        }
    }
    
    public void timedUpdateThread() {
    }

    public void initNeighbors(String file)
    {
        // Read neighbors from file and store them in the initial dv
        return;
    }
    
}
