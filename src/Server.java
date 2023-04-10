import message.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static message.MessageType.*;

public class Server {

    private static final int DEFAULT_PORT = 5555;
    private static final String CONFIG_FILE_PATH = "src/config.txt";

    private static final long IDLE_TIME_MILLIS = 3000;

    private volatile boolean isConnected = false;

    private final DatagramSocket socket;

    private final Map<String, Map<String, Integer>> initialTable = new HashMap<>(); // maps from node to its initial DV table.

    private final Map<String, List<String>> neighborMap = new HashMap<>(); // maps from node to its neighbors

    private final Map<String, InetSocketAddress> addressMap = new HashMap<>(); // maps from node to its address

    private Server() throws IOException {
        socket = new DatagramSocket(DEFAULT_PORT);
        processConfigFile();
    }

    // construct mapping of neighbors and initial dv table for response message
    private void processConfigFile() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE_PATH));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] data = line.replaceAll(":", "")
                    .replaceAll("<", "")
                    .replaceAll(">", "")
                    .replaceAll(",", "")
                    .split(" ");

            String curNode = data[0];

            // process a list of neighbors and initial DV table
            List<String> neighbors = new ArrayList<>();
            Map<String, Integer> table = new HashMap<>();
            table.put(curNode, 0);
            for (int i = 1; i < data.length; i += 2) {
                String neighbor = data[i];
                int weight = Integer.parseInt(data[i + 1]);

                table.put(neighbor, weight);
                if (weight == -1) continue;
                neighbors.add(neighbor);
            }
            neighborMap.put(curNode, neighbors);
            initialTable.put(curNode, table);
        }
    }

    public void accept() {
        isConnected = true;
        // listen to 6 nodes and save its address
        while (addressMap.size() < initialTable.size()) {
            Message recvMessage = Message.recvMessage(socket);

            if (!recvMessage.isType(JOIN)) continue;

            addressMap.put(recvMessage.getRouterID(), recvMessage.getAddress());
        }
        // Send back the initial DV tables back to nodes
        initialTable.forEach((nodeID, neighbors) -> {
            InetSocketAddress nodeAddress = addressMap.get(nodeID);
            Map<String, Integer> initialDV = initialTable.get(nodeID);

            Message message = new Message(RESPONSE, nodeID, nodeAddress, initialDV);
            Message.sendMessage(socket, message);
        });
    }

    public void listen() {
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
        Thread listenThread = new Thread(() -> {
            while (isConnected) {
                Message recvMessage = Message.recvMessage(socket);
                startTime.set(System.currentTimeMillis());

                if (!recvMessage.isType(UPDATE)) continue;

                System.out.println(recvMessage.getRouterID() + " forward its table to " + neighborMap.get(recvMessage.getRouterID()) + ":" + recvMessage.getDvTable());

                List<String> neighborList = neighborMap.get(recvMessage.getRouterID());
                multicast(neighborList, recvMessage);
            }
        });

        listenThread.start();
        while (System.currentTimeMillis() - startTime.get() <= IDLE_TIME_MILLIS);
        listenThread.interrupt();

        Message terminateMessage = new Message(TERMINATE, "", null);
        Collection<String> nodeList = addressMap.keySet();
        multicast(nodeList, terminateMessage);
    }

    private void multicast(Collection<String> recipients, Message message) {
        recipients.forEach(destinationID -> {
            message.setRouterID(destinationID);
            message.setAddress(addressMap.get(destinationID));
            Message.sendMessage(socket, message);
        });
    }

    public static void run() throws IOException {
        Server server = new Server();
        // accept initial connections
        System.out.println("start accepting");
        server.accept();
        System.out.println("finish accepting");
        // listen for updates
        server.listen();
    }

    public static void main(String[] args) throws IOException {
        Server.run();
    }
}
