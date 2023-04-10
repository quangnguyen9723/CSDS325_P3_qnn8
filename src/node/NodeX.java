package node;

public class NodeX extends AbstractNode{

    public NodeX() {
        super("x");
    }

    public static void main(String[] args) {
        // construct the node
        NodeX node = new NodeX();
        node.run();
    }
}
