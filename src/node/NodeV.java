package node;

import java.net.UnknownHostException;

public class NodeV extends AbstractNode{

    public NodeV() {
        super("v");
    }

    public static void main(String[] args) throws UnknownHostException {
        // construct the node
        NodeV node = new NodeV();
        node.run();
    }
}
