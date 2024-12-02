package client.Panel;

import javax.swing.*;
import java.awt.*;

public class LoadingPanel extends JPanel {
    JLabel loadingLabel;

    public LoadingPanel(){
        buildGUI();
    }

    public void buildGUI(){
        setLayout(new BorderLayout());
        setSize(1100, 600);
        loadingLabel = new JLabel("Loading...", SwingConstants.CENTER);
        add(loadingLabel);
    }
}
