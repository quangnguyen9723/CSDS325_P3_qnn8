package node;

public class NodeV extends AbstractNode{

    public NodeV() {
        super("v");
    }

    public static void main(String[] args) {
        // construct the node
        NodeV node = new NodeV();
        node.run();
    }
}
