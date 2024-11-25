package client.Panel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

// 게임 패널 + 채팅 패널
public class GameWithChatPanel extends JPanel {
    private GamePanel gamePanel;
    private ChatPanel chatPanel;

    public GameWithChatPanel(GamePanel gamePanel, ChatPanel chatPanel) {
        setLayout(new BorderLayout()); // BorderLayout으로 설정하여 꽉 차게 배치

        setSize(1150, 600);

        // GamePanel 및 ChatPanelBuilder 설정
        this.gamePanel = gamePanel;
        this.chatPanel = chatPanel;

        // JSplitPane으로 두 컴포넌트를 나란히 배치
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gamePanel, chatPanel);

        // JSplitPane에서 포커스 전환을 명확히 설정
        splitPane.setFocusable(false); // JSplitPane 자체가 포커스를 가지지 않도록 설정
        splitPane.setDividerLocation(800); // 분리선 초기 위치
        splitPane.setResizeWeight(0.7); // 게임 화면 비율
        splitPane.setOneTouchExpandable(true); // 분리선 확장 버튼

        // GamePanel에 포커스 리스너 추가
        gamePanel.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                gamePanel.requestFocusInWindow(); // GamePanel에 포커스 설정
            }
        });

        chatPanel.getT_input().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                gamePanel.setFocusable(false);
            }

            @Override
            public void focusLost(FocusEvent e) {
                // GamePanel의 포커스를 다시 활성화
                gamePanel.setFocusable(true);
            }
        });

        // 패널에 splitPane 추가
        add(splitPane, BorderLayout.CENTER);

        // 패널 표시
        setVisible(true);
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }
}