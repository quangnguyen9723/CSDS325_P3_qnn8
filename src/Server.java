import message.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static message.MessageType.*;

/**
 * Server class to handle the registration of nodes and forward the messages
 */
public class Server {

    private static final int DEFAULT_PORT = 5555;
    private static final String CONFIG_FILE_PATH = "src/config.txt";
    private static final String ALTERNATIVE_CONFIG_FILE_PATH = "./config.txt";
    private static final long IDLE_TIME_MILLIS = 1500;

    private final DatagramSocket socket;
    private final Map<String, Map<String, Integer>> initialTable = new HashMap<>(); // maps from node to its initial DV table.
    private final Map<String, List<String>> neighborMap = new HashMap<>(); // maps from node to its neighbors
    private final Map<String, InetSocketAddress> addressMap = new HashMap<>(); // maps from node to its address

    /**
     * Initialize a server with default port and process the config file
     */
    private Server() {
        try {
            socket = new DatagramSocket(DEFAULT_PORT);
            processConfigFile();
        } catch (IOException e) {
            System.out.println("error at default port or reading config.txt");
            throw new RuntimeException(e);
        }
    }

    /**
     * Construct mapping of neighbors and initial dv table
     */
    private void processConfigFile() throws IOException {
        BufferedReader reader;
        reader = createReader();
        String line;

        while ((line = reader.readLine()) != null) {
            processDataLine(line);
        }

        reader.close();
    }

    private BufferedReader createReader() throws FileNotFoundException {
        File configFile = new File(CONFIG_FILE_PATH);
        BufferedReader reader;

        if (configFile.exists()) {
            reader = new BufferedReader(new FileReader(CONFIG_FILE_PATH));
        } else {
            reader = new BufferedReader(new FileReader(ALTERNATIVE_CONFIG_FILE_PATH));
        }
        return reader;
    }

    private void processDataLine(String line) {
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

    /**
     * Wait for all nodes to sends JOIN message,
     * after all nodes join, send back RESPONSE message with initial dv table
     */
    public void accept() {
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

    /**
     * Listen for UPDATE messages and forward to neighbors
     */
    public void listen() {
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
        Thread listenThread = new Thread(() -> {
            while (true) {
                Message recvMessage = Message.recvMessage(socket);
                startTime.set(System.currentTimeMillis());

                if (!recvMessage.isType(UPDATE)) continue;

                System.out.format("%s forwards to %s with table %s\n", recvMessage.getRouterID(), neighborMap.get(recvMessage.getRouterID()), recvMessage.getDvTable());

                List<String> neighborList = neighborMap.get(recvMessage.getRouterID());
                multicast(neighborList, recvMessage);
            }
        });

        listenThread.setDaemon(true);
        listenThread.start();

        while (System.currentTimeMillis() - startTime.get() <= IDLE_TIME_MILLIS) {
            // do nothing
        }

        listenThread.interrupt();

        Message terminateMessage = new Message(TERMINATE, "", null);
        Collection<String> allNodes = addressMap.keySet();
        multicast(allNodes, terminateMessage);
    }

    /**
     * Sends message to specified nodes
     *
     * @param recipients specified nodes
     * @param message    message to send
     */
    private void multicast(Collection<String> recipients, Message message) {
        recipients.forEach(destinationID -> {
            message.setAddress(addressMap.get(destinationID));
            Message.sendMessage(socket, message);
        });
    }

    /**
     * Method to start a server
     */
    public static void start() {
        Server server = new Server();
        // accept initial connections
        System.out.println("start accepting...");
        server.accept();
        System.out.println("---listening--");
        // listen for updates
        server.listen();
        System.out.println("connection closed");
    }

    // ------------------------------------------------------------------------------------------------------------
    public static void main(String[] args) {
        Server.start();
    }
}