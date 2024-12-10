package client.Panel;

import client.service.GameClientService;
import data.ChatMsg;

import javax.swing.*;
import java.awt.*;

public class RoomPanel extends JPanel {
    private JTextField t_nickName, t_roomName, t_characterName, t_mode, t_team;
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

        int gameMode = msg.getGameMode();
        int team = msg.getTeam();

        // mode와 team 텍스트 필드 초기화
        t_mode = new JTextField(gameMode+"vs"+gameMode);
        t_team = new JTextField("팀 "+team);

        // 초기 상태 설정
        t_nickName.setEnabled(false);
        t_roomName.setEnabled(false);
        t_characterName.setEnabled(false);
        t_mode.setEditable(false);
        t_team.setEditable(false);

        infoPanel.add(new JLabel("아이디:"));
        infoPanel.add(t_nickName);
        infoPanel.add(new JLabel("방 이름:"));
        infoPanel.add(t_roomName);
        infoPanel.add(new JLabel("캐릭터 이름:"));
        infoPanel.add(t_characterName);
        infoPanel.add(new JLabel("모드:"));
        infoPanel.add(t_mode);
        infoPanel.add(new JLabel("팀:"));
        infoPanel.add(t_team);

        add(infoPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        b_exitRoom = new JButton("방 나가기");
        b_startGame = new JButton("게임방 입장");

        // 방 나가기 버튼 동작
        b_exitRoom.addActionListener(e -> service.exitRoom(
                t_roomName.getText(),
                t_nickName.getText(),
                t_characterName.getText(),
                gameMode,
                team
        ));

        // 게임방 입장 버튼 동작
        b_startGame.addActionListener(e -> service.requestStartGame(
                t_nickName.getText(),
                t_roomName.getText(),
                t_characterName.getText(),
                gameMode,
                team
        ));

        buttonPanel.add(b_exitRoom);
        buttonPanel.add(b_startGame);

        add(buttonPanel, BorderLayout.SOUTH);
    }

}