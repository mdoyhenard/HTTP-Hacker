package Network.View.Nodes;

import Network.Client;

import javax.swing.*;

public class ClientNode extends NetworkNode<Client> {

    public ClientNode() {
        super(new ImageIcon(ClientNode.class.getResource("/clientIcon.png")), Client::new);
    }
}
