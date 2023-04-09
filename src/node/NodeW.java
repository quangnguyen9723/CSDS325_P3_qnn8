package node;

import java.net.UnknownHostException;

public class NodeW extends AbstractNode{

    public NodeW() {
        super("w");
    }

    public static void main(String[] args) throws UnknownHostException {
        // construct the node
        NodeW node = new NodeW();
        node.run();
    }
}
