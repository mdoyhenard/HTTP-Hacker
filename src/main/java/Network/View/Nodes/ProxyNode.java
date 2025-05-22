package Network.View.Nodes;

import Network.Proxy;

import javax.swing.*;

public class ProxyNode extends NetworkNode<Proxy> {

    public ProxyNode() {
        super(new ImageIcon(ProxyNode.class.getResource("/proxyIcon.png")), Proxy::new);
    }

}
