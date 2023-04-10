import node.*;

import java.net.UnknownHostException;

public class Driver {

    public static void start(AbstractNode node) {
        new Thread(() -> {
            node.run();
        }).start();
    }

    public static void main(String[] args) {
        NodeU u = new NodeU();
        NodeV v = new NodeV();
        NodeW w = new NodeW();
        NodeX x = new NodeX();
        NodeY y = new NodeY();
        NodeZ z = new NodeZ();

        start(u);
        start(v);
        start(w);
        start(x);
        start(y);
        start(z);

    }
}
