package node;

import java.net.UnknownHostException;

public class NodeY extends AbstractNode{

    public NodeY() {
        super("y");
    }

    public static void main(String[] args) throws UnknownHostException {
        // construct the node
        NodeY node = new NodeY();
        node.run();
    }
}
