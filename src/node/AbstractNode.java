package node;

import utility.Message;

import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static utility.MessageType.*;

public class AbstractNode {
    public static final String SERVER_IP = "localhost";
    public static final int PORT = 5555;

    private InetSocketAddress serverAddress;

    private final String routerID;

    private final DatagramSocket socket;
    private Map<String, Integer> table;

    private final Map<String, String> nextHop = new HashMap<>();

    public AbstractNode(String routerID) {
        this.routerID = routerID;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("error creating socket");
            throw new RuntimeException(e);
        }
    }

    public void connect(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
        // Send JOIN message
        Message initMsg = new Message(JOIN, this.routerID, serverAddress);
        Message.sendMessage(socket, initMsg);
        // Receive RESPONSE message
        Message recvMsg = Message.recvMessage(socket);
        // sanity check
        if (!recvMsg.getType().equals(RESPONSE)) return;

        // init DV table
        this.table = recvMsg.getTable();
        // create neighbors table
        table.forEach((node, weight) -> {
            nextHop.put(node, null);
            if (weight < 0) {
                table.put(node, Integer.MAX_VALUE - 1000); // simulate infinity
            } else {
                nextHop.put(node, node);
            }
        });
    }

    public void init() {
        Message initMessage = new Message(UPDATE, this.routerID, this.serverAddress, this.table);
        Message.sendMessage(socket, initMessage);
    }


    public void update() {
        while (true) {
            // receive UPDATE
            Message recvMessage = Message.recvMessage(socket);
            // check UPDATE
            if (!recvMessage.getType().equals(UPDATE)) continue;
            // perform UPDATE
            String neighborID = recvMessage.getRouterID();
            Map<String, Integer> recvTable = recvMessage.getTable();
            AtomicBoolean isChanged = new AtomicBoolean(false);
            // Bellman-Ford
            table.forEach((node, weight) -> {
                // go directly
                int oldWeight = table.get(node);
                // go through the neighbor
                int newWeight = table.get(neighborID) + recvTable.get(node);
                if (newWeight < oldWeight) {
                    isChanged.set(true);
                    table.put(node, newWeight);
                    nextHop.put(node, neighborID);
                }

            });
            // no change
            if (!isChanged.get()) continue;

            // some change

            System.out.println(printTable());

            Message sendMsg = new Message(UPDATE, routerID, serverAddress, this.table);
            Message.sendMessage(socket, sendMsg);
        }
    }

    public String printTable() {
        StringBuilder sb = new StringBuilder();
        sb.append(routerID).append(": ");
        table.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey())
                        .append("-").append(e.getValue())
                        .append("-").append(nextHop.get(e.getKey()))
                        .append(" "));
        return sb.toString();
    }

    public void run() throws UnknownHostException {
        // send JOIN message
        this.connect(new InetSocketAddress(InetAddress.getByName(SERVER_IP), PORT));
        // send the first time
        this.init();
        // enter loop to UPDATE DV table
        this.update();
    }

}
