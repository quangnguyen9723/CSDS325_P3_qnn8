package node;

public class NodeZ extends AbstractNode{

    public NodeZ() {
        super("z");
    }

    public static void main(String[] args) {
        // construct the node
        NodeZ node = new NodeZ();
        node.run();
    }
}
