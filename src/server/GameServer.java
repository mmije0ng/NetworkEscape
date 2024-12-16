package server;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class GameServer extends JFrame {
    private GameServerService serverService; // 서버 로직
    private ServerPanel serverPanel; // 서버 패널

    public GameServer(String ipAddress, int port) {
        super("Game Server");

        serverService = new GameServerService(ipAddress, port);
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

    public static void main(String[] args) throws UnknownHostException {
        String ipAddress = InetAddress.getLocalHost().getHostAddress(); // 기본 ip 주소
        int port = 54321; // 기본 포트

        new GameServer(ipAddress, port);
    }
}
