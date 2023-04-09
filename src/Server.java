import utility.Message;

import java.io.*;
import java.net.*;
import java.util.*;

import static utility.MessageType.*;

public class Server {

    private static final int PORT = 5555;
    private static final String CONFIG_FILE_PATH = "src/config.txt";
    private final DatagramSocket socket;

    private final Map<String, Map<String, Integer>> initialTable = new HashMap<>(); // maps from node to its initial DV table.

    private final Map<String, List<String>> neighborMap = new HashMap<>(); // maps from node to its neighbors

    private final Map<String, InetSocketAddress> addressMap = new HashMap<>(); // maps from node to its address

    private Server() throws IOException {
        socket = new DatagramSocket(PORT);
        processConfigFile();
    }

    // construct mapping of neighbors and initial dv table for response message
    private void processConfigFile() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE_PATH));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.replaceAll(":", "")
                    .replaceAll("<", "")
                    .replaceAll(">", "")
                    .replaceAll(",", "");
            String[] data = line.split(" ");
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
        // listen to 6 nodes and save its address
        while (addressMap.size() < initialTable.size()) {
            // Receive a message
            Message recvMsg = Message.recvMessage(socket);
            // Check for JOIN message
            if (!recvMsg.getType().equals(JOIN)) {
                continue;
            }
            // Populate the IP and port
            addressMap.put(recvMsg.getRouterID(), recvMsg.getAddress());
        }
        // Send back the initial DV tables back to nodes
        initialTable.forEach((id, neighbors) -> {
            Message sendMsg = new Message(RESPONSE, id, addressMap.get(id), initialTable.get(id));
            Message.sendMessage(socket, sendMsg);
        });
    }

    public void listen() {
        while (true) {
            // Receiving incoming messages
            Message recvMessage = Message.recvMessage(socket);
            // Check for UPDATE message
            if (!recvMessage.getType().equals(UPDATE)) continue;
            // forward the message to its neighbor
            System.out.println(recvMessage.getRouterID() + " forward its table to " + neighborMap.get(recvMessage.getRouterID()) + ":" + recvMessage.getTable());
            neighborMap.get(recvMessage.getRouterID())
                    .forEach(neighbor -> {
                        // set destination address
                        recvMessage.setAddress(addressMap.get(neighbor));
                        Message.sendMessage(socket, recvMessage);
                    });
        }
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
