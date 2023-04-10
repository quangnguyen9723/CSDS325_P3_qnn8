package message;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;

public final class Message implements Serializable {

    private String routerID; // from a router if UPDATE or JOIN, or to a router if RESPONSE

    private InetSocketAddress address; // srcAddress if received, or destAddress if sending

    private final MessageType messageType;

    private final Map<String, Integer> dvTable;

    // format for UPDATE and RESPONSE messages
    public Message(MessageType messageType, String routerID, InetSocketAddress address, Map<String, Integer> dvTable) {
        this.messageType = messageType;
        this.routerID = routerID;
        this.address = address;
        this.dvTable = dvTable;
    }

    // format for JOIN message
    public Message(MessageType messageType, String routerID, InetSocketAddress address) {
        this(messageType, routerID, address, null);
    }

    public String getRouterID() {
        return routerID;
    }

    public void setRouterID(String routerID) {
        this.routerID = routerID;
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
        return this.messageType.equals(type);
    }

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
