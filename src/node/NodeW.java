package node;

public class NodeW extends AbstractNode{

    public NodeW() {
        super("w");
    }

    public static void main(String[] args) {
        // construct the node
        NodeW node = new NodeW();
        node.run();
    }
}
