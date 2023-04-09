package utility;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;

public final class Message implements Serializable {

    private final String routerID;

    private InetSocketAddress address; // srcAddress if received, or destAddress if sending

    private final MessageType type;

    private final Map<String, Integer> table;

    // format for UPDATE and RESPONSE messages
    public Message(MessageType type, String routerID, InetSocketAddress address, Map<String, Integer> table) {
        this.type = type;
        this.routerID = routerID;
        this.address = address;
        this.table = table;
    }

    // format for JOIN message
    public Message(MessageType type, String routerID, InetSocketAddress address) {
        this(type, routerID, address, null);
    }

    public String getRouterID() {
        return routerID;
    }

    public MessageType getType() {
        return type;
    }

    public Map<String, Integer> getTable() {
        return table;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public static Message recvMessage(DatagramSocket socket) {
        // Receive a DatagramPacket
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            System.out.println("error receiving message");
            throw new RuntimeException(e);
        }

        // Deserialize the message from the received packet
        Message msg;
        try (
                ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData());
                ObjectInputStream in = new ObjectInputStream(bis)
        ) {
            msg = (Message) in.readObject();
        } catch (IOException e) {
            System.out.println("error creating input stream");
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            System.out.println("error casting class");
            throw new RuntimeException(e);
        }
        // set address of sender
        msg.setAddress(new InetSocketAddress(packet.getAddress(), packet.getPort()));
        return msg;
    }

    public static void sendMessage(DatagramSocket socket, Message msg) {
        // Serialize the object to a byte array
        byte[] data;
        try (
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos)
        ) {
            out.writeObject(msg);
            data = bos.toByteArray();
        } catch (IOException e) {
            System.out.println("error creating output stream or writing object");
            throw new RuntimeException(e);
        }

        DatagramPacket packet = new DatagramPacket(data, data.length, msg.getAddress());

        // Send the DatagramPacket
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("error sending message");
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "routerID:" + routerID + " type:" + type + " address:" + address + " table: " + table;
    }
}
