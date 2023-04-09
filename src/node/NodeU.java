package node;

import java.net.UnknownHostException;

public class NodeU extends AbstractNode{

    public NodeU() {
        super("u");
    }

    public static void main(String[] args) throws UnknownHostException {
        // construct the node
        NodeU node = new NodeU();
        node.run();
    }
}
