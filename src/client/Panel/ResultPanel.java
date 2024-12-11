package client.Panel;

import data.ChatMsg;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ResultPanel extends JPanel {
    private String roomName, nickName;
    private int team, point, gameMode, winTeam, winPoint;
    private Map<String, String> winners; // 게임 승자 맵 (닉네임, 캐릭터)
    private ObjectOutputStream out; // 서버와의 통신을 위한 출력 스트림
    private Map<String, Image> characterImages = new HashMap<>(); // 캐릭터 이미지 맵

    private JButton b_exitRoom;

    public ResultPanel(String roomName, String nickName, int gameMode, int team, int point, int winTeam, int winPoint, Map<String, String> winners, ObjectOutputStream out) {
        this.roomName = roomName;
        this.nickName = nickName;
        this.gameMode=gameMode;
        this.team = team;
        this.point = point;
        this.winTeam = winTeam;
        this.winPoint = winPoint;
        this.winners = winners;
        this.out = out;

        // 캐릭터 이미지 로드
        characterImages.put("fire", createImageIcon("/image/character/fire.png").getImage());
        characterImages.put("water", createImageIcon("/image/character/water.png").getImage());
        characterImages.put("엠버", createImageIcon("/image/character/ember.png").getImage());
        characterImages.put("웨이드", createImageIcon("/image/character/wade.png").getImage());
        characterImages.put("버럭이", createImageIcon("/image/character/anger.png").getImage());
        characterImages.put("슬픔이", createImageIcon("/image/character/sad.png").getImage());

        buildGUI();
    }

    private void buildGUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 60));

        // 상단 정보 패널 구성
        add(createInfoPanel(), BorderLayout.NORTH);

        // 중앙 승자 패널 구성
        add(createWinnerPanel(), BorderLayout.CENTER);

        // 하단 결과 표시 패널 구성
        add(createBottomPanel(), BorderLayout.SOUTH);
    }


    // 상단 정보 패널 생성
    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS)); // BoxLayout으로 변경
        infoPanel.setOpaque(false);

        // 제목 라벨
        JLabel titleLabel = new JLabel("Game Result");
        titleLabel.setFont(new Font("돋움", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 가운데 정렬
        infoPanel.add(titleLabel);

        // 제목 밑 간격 추가 (예: 20px)
        infoPanel.add(Box.createRigidArea(new Dimension(0, 50)));

        // 승리/패배 메시지
        String resultMessage = Objects.equals(team, winTeam) ? nickName + "님은 승리하셨습니다." : nickName + "님은 패배하셨습니다.";
        JLabel resultLabel = new JLabel(resultMessage);
        resultLabel.setFont(new Font("돋움", Font.PLAIN, 16));
        resultLabel.setForeground(Color.WHITE);
        resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 가운데 정렬
        infoPanel.add(resultLabel);

        // 간격 추가
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 10px의 수직 간격

        // 점수 정보
        JLabel scoreLabel = new JLabel(nickName + "님의 점수: " + point);
        scoreLabel.setFont(new Font("돋움", Font.PLAIN, 16));
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 가운데 정렬
        infoPanel.add(scoreLabel);

        // 간격 추가
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 10px의 수직 간격

        // 승리 점수 정보
        String winnerScoreMessage = gameMode == 2 ? "승리 팀의 점수: " + winPoint : "승리 플레이어의 점수: " + winPoint;
        JLabel winnerScoreLabel = new JLabel(winnerScoreMessage);
        winnerScoreLabel.setFont(new Font("돋움", Font.PLAIN, 16));
        winnerScoreLabel.setForeground(Color.WHITE);
        winnerScoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 가운데 정렬
        infoPanel.add(winnerScoreLabel);

        return infoPanel;
    }

    // 중앙 승자 패널 생성
    private JPanel createWinnerPanel() {
        JPanel winnerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 60, 80));
        winnerPanel.setOpaque(false);

        if (winners != null && !winners.isEmpty()) {
            for (Map.Entry<String, String> entry : winners.entrySet()) {
                String nickname = entry.getKey();
                String character = entry.getValue();

                // 개별 승자 패널 생성
                JPanel individualPanel = new JPanel();
                individualPanel.setLayout(new BoxLayout(individualPanel, BoxLayout.Y_AXIS)); // 수직 배치
                individualPanel.setOpaque(false);

                // 캐릭터 이미지
                Image characterImage = characterImages.get(character);
                if (characterImage != null) {
                    JLabel imageLabel = new JLabel(new ImageIcon(characterImage.getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
                    imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 중앙 정렬
                    individualPanel.add(imageLabel);
                }

                // 이미지와 닉네임 사이 간격 추가
                individualPanel.add(Box.createRigidArea(new Dimension(0, 20)));

                // 닉네임 라벨
                JLabel nameLabel = new JLabel("승리 플레이어: " + nickname);
                nameLabel.setFont(new Font("돋움", Font.BOLD, 16));
                nameLabel.setForeground(Color.WHITE);
                nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 중앙 정렬
                individualPanel.add(nameLabel);

                winnerPanel.add(individualPanel);
            }
        } else {
            JLabel noWinnerLabel = new JLabel("No winners");
            noWinnerLabel.setFont(new Font("돋움", Font.BOLD, 16));
            noWinnerLabel.setForeground(Color.WHITE);
            noWinnerLabel.setHorizontalAlignment(SwingConstants.CENTER);
            winnerPanel.add(noWinnerLabel);
        }

        return winnerPanel;
    }

    // 하단 결과 및 종료 패널 생성
    private JPanel createBottomPanel() {
        // Bottom Panel Wrapper
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setOpaque(false);
        wrapperPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0)); // 아래 20px 여백 추가

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS)); // BoxLayout으로 설정 (수직 정렬)
        bottomPanel.setOpaque(false);

        // 결과 메시지
        String resultMessage = team == winTeam ? "You Win!" : "You Loose";
        JLabel resultLabel = new JLabel(resultMessage, SwingConstants.CENTER);
        resultLabel.setFont(new Font("돋움", Font.BOLD, 18));
        resultLabel.setForeground(Color.WHITE);
        resultLabel.setBackground(new Color(140, 0, 255));
        resultLabel.setOpaque(true);
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 수평 가운데 정렬
        resultLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 종료하기 버튼
        b_exitRoom = new JButton("종료하기");
        b_exitRoom.setAlignmentX(Component.CENTER_ALIGNMENT); // 수평 가운데 정렬
        b_exitRoom.addActionListener(e -> sendEXIT());

        // 컴포넌트 추가
        bottomPanel.add(resultLabel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 간격 추가
        bottomPanel.add(b_exitRoom);

        // BottomPanel을 WrapperPanel에 추가
        wrapperPanel.add(bottomPanel, BorderLayout.CENTER);

        return wrapperPanel;
    }

    // 이미지 경로를 가져와 ImageIcon 생성
    public ImageIcon createImageIcon(String imagePath) {
        // 클래스패스에서 리소스를 로드
        URL url = this.getClass().getResource(imagePath);

        if (url == null) {
            System.err.println("리소스를 찾을 수 없습니다: " + imagePath);
        }

        // ImageIcon 생성
        return new ImageIcon(url);
    }

    // 서버로 방 퇴장 메시지 전송
    private synchronized void sendEXIT() {
        if (out != null) {
            try {
                ChatMsg chatMsg = new ChatMsg.Builder("EXIT")
                        .roomName(roomName)
                        .nickname(nickName)
                        .build();

                out.writeObject(chatMsg);
                out.flush();
            } catch (IOException e) {
                System.out.println("sendEXIT 오류> " + e.getMessage());
            }
        }
    }
}
