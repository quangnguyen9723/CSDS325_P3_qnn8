package node;

import java.net.UnknownHostException;

public class NodeZ extends AbstractNode{

    public NodeZ() {
        super("z");
    }

    public static void main(String[] args) throws UnknownHostException {
        // construct the node
        NodeZ node = new NodeZ();
        node.run();
    }
}
