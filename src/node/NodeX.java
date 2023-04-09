package node;

import java.net.UnknownHostException;

public class NodeX extends AbstractNode{

    public NodeX() {
        super("x");
    }

    public static void main(String[] args) throws UnknownHostException {
        // construct the node
        NodeX node = new NodeX();
        node.run();
    }
}
