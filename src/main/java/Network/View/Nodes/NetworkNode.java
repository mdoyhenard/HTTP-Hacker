package Network.View.Nodes;

import Network.Connection;
import Network.NetworkComponent;
import Network.Proxy;
import Utils.HttpParser;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class NetworkNode<T extends NetworkComponent> extends JLabel implements Selectionable{

    private boolean active = false;
    protected T component;

    public NetworkNode(ImageIcon icon, Supplier<T> supplier) {
        super(scaleIcon(icon,0.1));
        setSize(icon.getIconWidth(), icon.getIconHeight());
        this.component = supplier.get();
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            setBorder(BorderFactory.createLineBorder(Color.RED, 3));
        } else {
            setBorder(null);
        }
    }

    public static ImageIcon scaleIcon(ImageIcon icon, double scaleFactor) {
        Image image = icon.getImage();
        int newW = (int)(icon.getIconWidth() * scaleFactor);
        int newH = (int)(icon.getIconHeight() * scaleFactor);
        Image scaledImage = image.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    public T getComponent(){
        return component;
    }
}
