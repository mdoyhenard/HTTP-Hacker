package Network.View.Nodes;

import Network.Server;

import javax.swing.*;

public class ServerNode extends NetworkNode<Server> {

    public ServerNode() {
        super(new ImageIcon(ServerNode.class.getResource("/serverIcon.png")), Server::new);
    }
}
