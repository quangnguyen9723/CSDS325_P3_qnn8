package node;

public class NodeY extends AbstractNode{

    public NodeY() {
        super("y");
    }

    public static void main(String[] args) {
        // construct the node
        NodeY node = new NodeY();
        node.run();
    }
}
