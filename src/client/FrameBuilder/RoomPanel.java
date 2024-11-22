package client.FrameBuilder;

import client.service.GameClientService;
import data.ChatMsg;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;

public class RoomPanel extends JPanel {
    private JTextField t_nickName, t_roomName, t_characterName;
    private JComboBox<String> modeComboBox, teamComboBox;
    private JButton b_exitRoom, b_startGame;
    private GameClientService service;

    public RoomPanel(GameClientService service, ChatMsg msg) {
        this.service = service;
        buildPanel(msg);
    }

    private void buildPanel(ChatMsg msg) {
        setLayout(new BorderLayout());

        JPanel infoPanel = new JPanel(new GridLayout(2, 6));
        t_nickName = new JTextField(msg.getNickname());
        t_roomName = new JTextField(msg.getRoomName());
        t_characterName = new JTextField(msg.getCharacter());
        modeComboBox = new JComboBox<>(new String[]{"모드 1", "모드 2"});
        teamComboBox = new JComboBox<>(new String[]{"팀 1", "팀 2"});
        teamComboBox.setEnabled(false);
        
        t_nickName.setEnabled(false);
        t_roomName.setEnabled(false);
        t_characterName.setEnabled(false);
        
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
        b_exitRoom = new JButton("방 나가기");
        b_startGame = new JButton("게임방 입장");

//         접속 끊기
        b_exitRoom.addActionListener(e -> service.exitRoom(
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

        buttonPanel.add(b_exitRoom);
        buttonPanel.add(b_startGame);

        add(buttonPanel, BorderLayout.SOUTH);
    }

}