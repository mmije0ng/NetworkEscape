package client;

import client.FrameBuilder.GamePanel;
import client.FrameBuilder.RoomPanel;
import client.FrameBuilder.LoginFrameBuilder;
import client.FrameBuilder.MainFrameBuilder;
import client.service.GameClientService;
import data.ChatMsg;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;

public class GameClient extends JFrame {
    private GameClientService gameClientService;

    private RoomPanel roomPanel;
    private LoginFrameBuilder loginFrameBuilder;
    private MainFrameBuilder mainFrameBuilder;
    private GamePanel gamePanel;
    private JTextPane t_display;
    private DefaultStyledDocument document;

    public GameClient(String serverAddress, int serverPort) {
        super("Network Escape");

        gameClientService = new GameClientService(this, serverAddress, serverPort);
//        gameStartPanel = new GameStartPanel(gameClientService);
        loginFrameBuilder = new LoginFrameBuilder(gameClientService, serverAddress, serverPort);
        roomPanel = new RoomPanel(gameClientService, new ChatMsg.Builder("").build());
        mainFrameBuilder = new MainFrameBuilder(gameClientService, "");

        buildGUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void buildGUI() {
        setSize(800, 600);
        setLocation(100, 100);
        setLayout(new BorderLayout());

        document = new DefaultStyledDocument();
        t_display = new JTextPane(document);
        t_display.setEditable(false);

        add(loginFrameBuilder,BorderLayout.CENTER);
//        add(new JScrollPane(t_display), BorderLayout.CENTER);
//        add(gameStartPanel, BorderLayout.SOUTH);
    }

    public void printRoom(ChatMsg msg){
        mainFrameBuilder.printLobbyList(msg.getRoomName());
    }
    // 텍스트 메시지 출력


    // 이미지 메시지 출력
    public void printDisplay(ImageIcon icon) {
        if (icon.getIconWidth() > 400) {
            Image img = icon.getImage().getScaledInstance(400, -1, Image.SCALE_SMOOTH);
            icon = new ImageIcon(img);
        }
        t_display.setCaretPosition(t_display.getDocument().getLength());
        t_display.insertIcon(icon);
        printDisplay("");
    }

    public void printDisplay(String msg) {
        try {
            int len = t_display.getDocument().getLength();
            document.insertString(len, msg + "\n", null);
            t_display.setCaretPosition(len);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void printDisplayWithRoom(String msg) {
        try {
            int len = t_display.getDocument().getLength();
            document.insertString(len, msg + "\n", null);
            t_display.setCaretPosition(len);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void startMainPanel(GameClientService service, ChatMsg msg){
        getContentPane().removeAll();

        mainFrameBuilder = new MainFrameBuilder(service,msg.getNickname());
        add(mainFrameBuilder);
        mainFrameBuilder.setVisible(true);
        mainFrameBuilder.requestFocusInWindow();

        revalidate();
        repaint();

    }

    public void startRoomPanel(GameClientService service, ChatMsg msg){

        getContentPane().removeAll();
        add(t_display,BorderLayout.CENTER);

        roomPanel = new RoomPanel(service,msg);
        add(roomPanel,BorderLayout.SOUTH);

        roomPanel.requestFocusInWindow();

        revalidate();
        repaint();

        System.out.println("startRoomPanel msg code: "+msg.getCode()+", characterName: "+msg.getCharacter());
    }

   // 게임 시작 패널 -> 게임 패널로 전환
    public void startGamePanel(ChatMsg msg) {

        getContentPane().removeAll();
        gamePanel = new GamePanel(
                msg.getNickname(),
                msg.getCharacter(),
                msg.getRoomName(),
                msg.getGameMode(),
                msg.getTeam(),
                gameClientService.getOutStream()
        );
        gamePanel.updateOtherPlayerPosition(msg.getNickname(), msg.getCharacter(), 10, getHeight()-40);

        add(gamePanel);

        gamePanel.requestFocusInWindow();

        revalidate();
        repaint();
    }

    public GamePanel getGamePanel(){
        return gamePanel;
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;
        new GameClient(serverAddress, serverPort);
    }
}
