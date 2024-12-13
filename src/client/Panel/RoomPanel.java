package client.Panel;

import client.service.GameClientService;
import data.ChatMsg;

import javax.swing.*;
import java.awt.*;

public class RoomPanel extends JPanel {
    private JButton b_exitRoom, b_startGame;
    private GameClientService service;
    private String roomName, nickName, character;
    private int mode, team;

    public RoomPanel(GameClientService service, ChatMsg msg) {
        this.service = service;
        this.roomName = msg.getRoomName();
        this.nickName = msg.getNickname();
        this.character = msg.getCharacter();
        this.mode = msg.getGameMode();
        this.team = msg.getTeam();

        buildPanel();
    }

    private void buildPanel() {
        setLayout(new BorderLayout());

        JPanel infoPanel = new JPanel(new GridLayout(2, 6));

        infoPanel.add(new JLabel("아이디: "+nickName));
        infoPanel.add(new JLabel("방 이름: "+roomName));
        infoPanel.add(new JLabel("캐릭터 이름: "+character));
        infoPanel.add(new JLabel("모드: "+mode+"vs"+mode));
        infoPanel.add(new JLabel("팀 "+team));

        add(infoPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        b_exitRoom = new JButton("방 나가기");
        b_startGame = new JButton("게임방 입장");

        // 방 나가기 버튼 동작
        b_exitRoom.addActionListener(e -> service.exitRoom(
                roomName,
                nickName,
                character,
                mode,
                team
        ));

        // 게임방 입장 버튼 동작
        b_startGame.addActionListener(e -> service.requestStartGame(
                roomName,
                nickName,
                character,
                mode,
                team
        ));

        buttonPanel.add(b_exitRoom);
        buttonPanel.add(b_startGame);

        add(buttonPanel, BorderLayout.SOUTH);
    }

}