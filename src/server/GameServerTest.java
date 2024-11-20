package server;

import javax.swing.*;
import java.awt.*;

public class GameServerTest extends JFrame {
    private GameServerService serverService; // 서버 로직
    private ServerPanel serverPanel; // 서버 패널

    public GameServerTest(int port) {
        super("Game Server");

        serverService = new GameServerService(port);
        serverPanel = new ServerPanel(serverService);

        buildGUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void buildGUI() {
        setSize(450, 500);
        setLocation(900, 100);
        setLayout(new BorderLayout());

        add(serverPanel.createDisplayPanel(), BorderLayout.CENTER);
        add(serverPanel.createControlPanel(), BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        int port = 54321;
        new GameServerTest(port);
    }
}
