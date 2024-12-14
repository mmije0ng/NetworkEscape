package client;

import client.service.GameClientService;
import data.ChatMsg;
import client.Panel.*;
import data.GameMsg;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Stack;

public class GameClient extends JFrame {
    private GameClientService gameClientService;

    private RoomPanel roomPanel;
    private LoginPanel loginPanel;
    private MainPanel mainPanel;
    private LoadingPanel loadingPanel;
    private JTextPane t_display;
    private DefaultStyledDocument document;
    private GameWithChatPanel gameWithChatPanel;

    private Stack<GameWithChatPanel> gamePanelStack = new Stack<>(); // GameWithChatPanel 스택

    public GameClient(String serverAddress, int serverPort) {
        super("미로 대탈출");

        gameClientService = new GameClientService(this, serverAddress, serverPort);
//        gameStartPanel = new GameStartPanel(gameClientService);
        loginPanel = new LoginPanel(gameClientService, serverAddress, serverPort);
        roomPanel = new RoomPanel(gameClientService, new ChatMsg.Builder("").build());
        mainPanel = new MainPanel(gameClientService, "");

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

        add(loginPanel,BorderLayout.CENTER);
//        add(new JScrollPane(t_display), BorderLayout.CENTER);
//        add(gameStartPanel, BorderLayout.SOUTH);
    }

    public void printRoom(ChatMsg msg){
        mainPanel.printLobbyList(msg);
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

    public void startMainPanel(GameClientService service, ChatMsg msg){
        getContentPane().removeAll();

        mainPanel = new MainPanel(service,msg.getNickname());
        add(mainPanel);
        mainPanel.setVisible(true);
        mainPanel.requestFocusInWindow();

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

        System.out.println("startRoomPanel msg code: "+msg.getCode()+", characterName: "+msg.getCharacter()+", team: "+msg.getTeam()+", mode: "+msg.getGameMode());
    }

   // NEXT_MAP 메시지를 받아 로딩 화면 표시
   public void startLoadingPanel(GameMsg msg, OutputStream out) {
       // 기존 GameWithChatPanel 스택에 저장
       if (gameWithChatPanel != null) {
           gamePanelStack.push(gameWithChatPanel);
       }

       // 로딩 패널 표시
       getContentPane().removeAll();
       loadingPanel = new LoadingPanel();
       add(loadingPanel);
       revalidate();
       repaint();

       // 일정 시간 후 로딩 화면 종료 및 GameWithChatPanel 복원
       Timer timer = new Timer(3000, e -> {
           if (!gamePanelStack.isEmpty()) {
               gameWithChatPanel = gamePanelStack.pop(); // 스택에서 꺼내기
               getContentPane().removeAll();

               add(gameWithChatPanel);

               gameWithChatPanel.getGamePanel().initializeNextMap(msg.getLevel());

               revalidate();
               repaint();
           }
       });
       timer.setRepeats(false); // 한 번만 실행
       timer.start();
   }

    // 게임 시작 패널 -> 게임 패널+채팅 패널로 전환
    public void startGameWithChatPanel(ChatMsg msg, ObjectOutputStream out) {
        getContentPane().removeAll();

        setSize(1100, 600);

        // 새로운 GamePanel 생성
        GamePanel gamePanel = new GamePanel(
                msg.getNickname(),
                msg.getCharacter(),
                msg.getRoomName(),
                msg.getGameMode(),
                msg.getTeam(),
                1, // 처음 시작 레벨은 1
                out
        );

        // 새로운 ChatPanel 생성
        ChatPanel chatPanel = new ChatPanel(msg.getRoomName(), msg.getNickname(), msg.getCharacter(), msg.getGameMode(), msg.getTeam(), out);

        // 기존 GameWithChatPanel 스택에 저장
        if (gameWithChatPanel != null) {
            gamePanelStack.push(gameWithChatPanel);
        }

        // 새로운 GameWithChatPanel 생성
        gameWithChatPanel = new GameWithChatPanel(gamePanel, chatPanel);
        add(gameWithChatPanel);

        gamePanel.requestFocusInWindow();
        revalidate();
        repaint();
    }

    public void startLoginPanel(String serverAddress, int serverPort){
        getContentPane().removeAll();
        loginPanel = new LoginPanel(gameClientService,serverAddress,serverPort);

        add(loginPanel);
        loginPanel.requestFocusInWindow();

        revalidate();
        repaint();

    }

    public GameWithChatPanel getGameWithChatPanel() {
        return gameWithChatPanel;
    }
    public MainPanel getMainPanel() {return mainPanel;}

    // 게임 결과 패널
    public void startResultPanel(GameMsg msg, ObjectOutputStream out) {
        // 기존 GameWithChatPanel 스택에 저장
        if (gameWithChatPanel != null) {
            gamePanelStack.push(gameWithChatPanel);
        }

        // 로딩 패널 표시
        getContentPane().removeAll();
        loadingPanel = new LoadingPanel();
        add(loadingPanel);
        revalidate();
        repaint();

        // 일정 시간 후 로딩 패널 종료 및 ResultPanel 표시
        Timer timer = new Timer(3000, e -> {
            getContentPane().removeAll();

            setSize(1100, 600);

            // ResultPanel 생성 및 추가
            ResultPanel resultPanel = new ResultPanel(
                    msg.getRoomName(),
                    msg.getNickname(),
                    msg.getGameMode(),
                    msg.getTeam(),
                    msg.getPoint(),
                    msg.getWinTeam(),
                    msg.getWinPoint(),
                    msg.getWinners(),
                    out
            );

            add(resultPanel);
            resultPanel.requestFocusInWindow();
            revalidate();
            repaint();
        });
        timer.setRepeats(false); // 한 번만 실행
        timer.start();
    }


    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;
        new GameClient(serverAddress, serverPort);
    }
}
