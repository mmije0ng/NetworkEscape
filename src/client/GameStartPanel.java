package client;

import data.ChatMsg;

import javax.swing.*;
import java.awt.*;

public class GameStartPanel extends JPanel {
    private JTextField t_nickName, t_roomName, t_characterName;
    private JComboBox<String> modeComboBox, teamComboBox;
    private JButton b_connect, b_disconnect, b_startGame;
    private GameClientService service;

    public GameStartPanel(GameClientService service) {
        this.service = service;
        buildPanel();
    }

    private void buildPanel() {
        setLayout(new BorderLayout());

        JPanel infoPanel = new JPanel(new GridLayout(2, 6));
        t_nickName = new JTextField("guest");
        t_roomName = new JTextField("Room1");
        t_characterName = new JTextField("fire");
        modeComboBox = new JComboBox<>(new String[]{"모드 1", "모드 2"});
        teamComboBox = new JComboBox<>(new String[]{"팀 1", "팀 2"});
        teamComboBox.setEnabled(false);

        modeComboBox.addActionListener(e -> teamComboBox.setEnabled(modeComboBox.getSelectedIndex() == 1));

        infoPanel.add(new JLabel("아이디:"));
        infoPanel.add(t_nickName);
        infoPanel.add(new JLabel("방 이름:"));
        infoPanel.add(t_roomName);
        infoPanel.add(new JLabel("캐릭터 이름:"));
        infoPanel.add(t_characterName);
        infoPanel.add(new JLabel("모드:"));
        infoPanel.add(modeComboBox);
        infoPanel.add(new JLabel("팀:"));
        infoPanel.add(teamComboBox);

        add(infoPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        b_connect = new JButton("접속하기");
        b_disconnect = new JButton("접속 끊기");
        b_startGame = new JButton("게임방 입장");

        // 접속하기
        b_connect.addActionListener(e -> service.connectToServer(
                t_nickName.getText(),
                t_roomName.getText(),
                t_characterName.getText(),
                modeComboBox.getSelectedIndex() + 1,
                teamComboBox.isEnabled() ? teamComboBox.getSelectedIndex() + 1 : 1
        ));

        // 접속 끊기
        b_disconnect.addActionListener(e -> service.disconnect(
                t_roomName.getText(),
                t_nickName.getText(),
                t_characterName.getText(),
                modeComboBox.getSelectedIndex() + 1,
                teamComboBox.isEnabled() ? teamComboBox.getSelectedIndex() + 1 : 1
        ));

        // 게임방 입장
        b_startGame.addActionListener(e -> service.requestStartGame(
                t_nickName.getText(),
                t_roomName.getText(),
                t_characterName.getText(),
                modeComboBox.getSelectedIndex() + 1,
                teamComboBox.isEnabled() ? teamComboBox.getSelectedIndex() + 1 : 1
        ));

        buttonPanel.add(b_connect);
        buttonPanel.add(b_disconnect);
        buttonPanel.add(b_startGame);

        add(buttonPanel, BorderLayout.SOUTH);
    }
}