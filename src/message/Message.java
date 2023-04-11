package message;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * An exchangeable message between nodes
 */
public final class Message implements Serializable {

    private final String routerID; // source ID if UPDATE or JOIN, or destination ID if RESPONSE

    private InetSocketAddress address; // srcAddress if received, or destAddress if sending

    private final MessageType messageType;

    private final Map<String, Integer> dvTable;

    /**
     * Message format for UPDATE and RESPONSE messages
     * @param messageType type of message
     * @param routerID source if UPDATE, or destination if RESPONSE
     * @param address destination address
     * @param dvTable forwarding dv table
     */
    public Message(MessageType messageType, String routerID, InetSocketAddress address, Map<String, Integer> dvTable) {
        this.messageType = messageType;
        this.routerID = routerID;
        this.address = address;
        this.dvTable = dvTable;
    }

    /**
     * Message format for JOIN messages
     * @param messageType type of message
     * @param routerID source if UPDATE, or destination if RESPONSE
     * @param address destination address
     */
    public Message(MessageType messageType, String routerID, InetSocketAddress address) {
        this(messageType, routerID, address, null);
    }

    public String getRouterID() {
        return routerID;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Map<String, Integer> getDvTable() {
        return dvTable;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }


    public boolean isType(MessageType type) {
        return this.messageType != null && this.messageType.equals(type);
    }

    /**
     * Receive message from a specified socket
     * @param socket listening socket
     * @return received message
     */
    public static Message recvMessage(DatagramSocket socket) {
        // receive a packet
        byte[] buffer = new byte[1024];
        DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(recvPacket);
        } catch (IOException e) {
            System.out.println("error receiving message");
            throw new RuntimeException(e);
        }

        // Deserialize the message from the received packet
        Message recvMessage;
        try (
                ByteArrayInputStream bis = new ByteArrayInputStream(recvPacket.getData());
                ObjectInputStream in = new ObjectInputStream(bis)
        ) {
            recvMessage = (Message) in.readObject();
        } catch (IOException e) {
            System.out.println("error creating input stream");
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            System.out.println("error casting class");
            throw new RuntimeException(e);
        }
        // set address of sender
        recvMessage.setAddress(new InetSocketAddress(recvPacket.getAddress(), recvPacket.getPort()));
        return recvMessage;
    }

    /**
     * Forwards a given message from sending socket
     * @param socket sending socket
     * @param message message to forward
     */
    public static void sendMessage(DatagramSocket socket, Message message) {
        // Serialize the object to a byte array
        byte[] sendData;
        try (
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos)
        ) {
            out.writeObject(message);
            sendData = bos.toByteArray();
        } catch (IOException e) {
            System.out.println("error creating output stream or writing object");
            throw new RuntimeException(e);
        }

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, message.getAddress());

        // Send the DatagramPacket
        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            System.out.println("error sending message");
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "routerID:" + routerID + " type:" + messageType + " address:" + address + " table: " + dvTable;
    }
}
