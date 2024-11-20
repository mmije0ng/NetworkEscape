package client;

import data.ChatMsg;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;

public class GameClientTest extends JFrame {
    private GameClientService gameClientService;
    private GameStartPanel gameStartPanel;
    private GamePanel gamePanel;
    private JTextPane t_display;
    private DefaultStyledDocument document;

    public GameClientTest(String serverAddress, int serverPort) {
        super("Network Escape");

        gameClientService = new GameClientService(this, serverAddress, serverPort);
        gameStartPanel = new GameStartPanel(gameClientService);

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

        add(new JScrollPane(t_display), BorderLayout.CENTER);
        add(gameStartPanel, BorderLayout.SOUTH);
    }

    // 텍스트 메시지 출력
    public void printDisplay(String msg) {
        try {
            int len = t_display.getDocument().getLength();
            document.insertString(len, msg + "\n", null);
            t_display.setCaretPosition(len);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

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

   // 게임 시작 패널 -> 게임 패널로 전환
    public void startGamePanel(ChatMsg msg) {
        remove(gameStartPanel);

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
        new GameClientTest(serverAddress, serverPort);
    }
}
