package node;

import message.Message;

import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static message.MessageType.*;

public class AbstractNode {
    public static final String DEFAULT_SERVER_IP = "localhost";
    public static final int DEFAULT_SERVER_PORT = 5555;

    public static final int INFINITY = Integer.MAX_VALUE / 2;

    private InetSocketAddress serverAddress;

    private final String routerID;

    private final DatagramSocket socket;
    private Map<String, Integer> dvTable;

    private final Map<String, String> nextHopMap = new HashMap<>();

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
        Message joinMessage = new Message(JOIN, this.routerID, serverAddress);
        Message.sendMessage(socket, joinMessage);

        Message recvMsg = Message.recvMessage(socket);
        boolean isResponseMessage = recvMsg.getMessageType().equals(RESPONSE);
        if (!isResponseMessage) return;

        this.dvTable = recvMsg.getDvTable();

        dvTable.forEach((nodeID, weight) -> {
            if (weight < 0) {
                dvTable.put(nodeID, INFINITY);
                nextHopMap.put(nodeID, null);
            } else {
                nextHopMap.put(nodeID, nodeID);
            }
        });
    }

    public void sendFirstRound() {
        Message initialUpdateMessage = new Message(UPDATE, this.routerID, this.serverAddress, this.dvTable);
        Message.sendMessage(socket, initialUpdateMessage);
    }


    public void update() {
        sendFirstRound();

        while (true) {
            Message recvMessage = Message.recvMessage(socket);

            if (recvMessage.isType(TERMINATE)) break;
            if (!recvMessage.isType(UPDATE)) continue;

            String neighborID = recvMessage.getRouterID();
            Map<String, Integer> recvTable = recvMessage.getDvTable();
            AtomicBoolean tableIsChanged = new AtomicBoolean(false);

            // Bellman-Ford
            dvTable.forEach((nodeID, weight) -> {
                int oldWeight = dvTable.get(nodeID);
                int newWeight = dvTable.get(neighborID) + recvTable.get(nodeID);

                if (newWeight < oldWeight) {
                    tableIsChanged.set(true);
                    dvTable.put(nodeID, newWeight);
                    nextHopMap.put(nodeID, neighborID);
                }

            });

            if (!tableIsChanged.get()) continue;

            System.out.println("Updated table:" + printTable());

            Message updateMessage = new Message(UPDATE, routerID, serverAddress, this.dvTable);
            Message.sendMessage(socket, updateMessage);
        }

    }

    public String printTable() {
        StringBuilder sb = new StringBuilder();
        dvTable.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(String.format(" <%s,%d,%s>",
                        e.getKey(),
                        e.getValue() == INFINITY ? -1 : e.getValue(),
                        nextHopMap.get(e.getKey()))));
        return sb.toString();
    }

    // ------------------------------------------------------------------------------------------------------------

    public void run() {
        InetSocketAddress serverAddress;
        try {
            serverAddress = new InetSocketAddress(InetAddress.getByName(DEFAULT_SERVER_IP), DEFAULT_SERVER_PORT);
        } catch (UnknownHostException e) {
            System.out.println("error connecting to <server_ip> or <server_port>");
            throw new RuntimeException(e);
        }

        System.out.println("Waiting for other nodes to join...");

        this.connect(serverAddress);

        System.out.println("NODE " + routerID);
        System.out.println("FORMAT: <Node, Cost, Next Hop>");
        System.out.println("Initial table: " + printTable() + "\n");

        this.update();
        System.out.println("\nStabilized table:" + printTable());
    }

}
