import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.List;

import static java.util.Map.Entry;

public final class Message implements Serializable {
    public enum MessageType {
        JOIN, UPDATE, RESPONSE
    }

    private final String routerID;

    private SocketAddress address; // srcAddress or destAddress

    private final MessageType type;

    private final List<Entry<String, Integer>> table;

    // format for UPDATE and RESPONSE messages
    public Message(MessageType type, String routerID, SocketAddress address, List<Entry<String, Integer>> table) {
        this.type = type;
        this.routerID = routerID;
        this.address = address;
        this.table = table;
    }

    // format for JOIN message
    public Message(MessageType type, String routerID, SocketAddress address) {
        this(type, routerID, address, null);
    }

    public String getRouterID() {
        return routerID;
    }

    public MessageType getType() {
        return type;
    }

    public List<Entry<String, Integer>> getTable() {
        return table;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public void setAddress(SocketAddress address) {
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
}
