package server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerPanel {
    private GameServerService serverService;
    private JTextArea t_display;
    private JButton startButton; // 시작 버튼
    private JButton stopButton; // 종료 버튼
    
    public ServerPanel(GameServerService serverService) {
        this.serverService = serverService;
    }

    public JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        t_display = new JTextArea();
        t_display.setEditable(false);
        serverService.setDisplayArea(t_display);

        panel.add(new JScrollPane(t_display), BorderLayout.CENTER);
        return panel;
    }

    public JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2));

        startButton = new JButton("서버 시작");
        stopButton = new JButton("서버 종료");

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                serverService.startServer();
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            }
        });

        stopButton.setEnabled(false);
        // 종료 버튼 클릭 시 서버 종료
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                serverService.serverDisconnect();
            }
        });

        panel.add(startButton);
        panel.add(stopButton);

        return panel;
    }
}
