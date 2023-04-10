package node;

public class NodeU extends AbstractNode{

    public NodeU() {
        super("u");
    }

    public static void main(String[] args) {
        // construct the node
        NodeU node = new NodeU();
        node.run();
    }
}
