package client.Panel;

import client.service.GameClientService;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class MainPanel extends JPanel{
    public JTextField t_lobbyName,t_lobbyPassword;
    public JTextArea t_lobbyList;
    public JButton b_playMode1, b_playMode2, b_createLobby, b_logout,b_enterLobby;

    public JButton blueCharacter1,blueCharacter2,blueCharacter3;
    public JButton redCharacter1,redCharacter2,redCharacter3;
    private DefaultStyledDocument document;
    private GameClientService service;

    private int playmode;
    private String userName;

    private String selectedCharacter; // 선택된 캐릭터 이름


    public MainPanel(GameClientService service, String username){
        this.service = service;
        this.userName=username;
        buildGUI(username);
    }
    public void buildGUI(String userName){

        this.setBounds(0,0,500,500);
        this.setLayout(new GridLayout(1,2));

        JPanel LobbyPanel,UserPanel;
        LobbyPanel = createLobbyPanel();
        UserPanel = createUserPanel(userName);

        this.add(LobbyPanel);
        this.add(UserPanel);


        this.setVisible(true);
    }
    private JPanel createLobbyPanel(){
        document = new DefaultStyledDocument();

        JPanel panel = new JPanel(new GridLayout(0,1));
        JPanel topPanel = new JPanel();
        JPanel bottomPanel = new JPanel(new BorderLayout());
        t_lobbyName = new JTextField(10);
        t_lobbyPassword = new JTextField(10);

        t_lobbyList = new JTextArea(document);
        JScrollPane scroll=new JScrollPane(t_lobbyList);

        t_lobbyList.setEditable(false);
        bottomPanel.add(new JLabel("방 목록"),BorderLayout.NORTH);
        bottomPanel.add(scroll,BorderLayout.CENTER);


        topPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 방 이름 필드
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel("방 이름"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        topPanel.add(t_lobbyName, gbc);

        // 비밀번호 필드
        gbc.gridx = 0;
        gbc.gridy = 1;
        topPanel.add(new JLabel("비밀번호"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        topPanel.add(t_lobbyPassword, gbc);

        // 게임 모드 버튼
        gbc.gridx = 0;
        gbc.gridy = 2;
        topPanel.add(new JLabel("게임 모드"), gbc);



        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        b_playMode1 = new JButton("1vs1");
        b_playMode2 = new JButton("2vs2");
        b_playMode1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                b_playMode1.setEnabled(false);
                b_playMode2.setEnabled(true);
                playmode=1;

            }
        });
        b_playMode2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                b_playMode1.setEnabled(true);
                b_playMode2.setEnabled(false);
                playmode=2;
            }
        });
        modePanel.add(b_playMode1);
        modePanel.add(b_playMode2);

        gbc.gridx = 1;
        gbc.gridy = 2;
        topPanel.add(modePanel, gbc);

        // 방 생성, 검색, 참가 버튼
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        b_createLobby = new JButton("방 생성");
        b_createLobby.setBackground(Color.ORANGE);

        b_createLobby.addActionListener(e->service.createRoom(
                userName,
                t_lobbyName.getText(),
                t_lobbyPassword.getText(),
                selectedCharacter,
                playmode
        ));

        topPanel.add(b_createLobby, gbc);

        gbc.gridy = 4;
        b_enterLobby = new JButton("방 참가");

        b_enterLobby.addActionListener(e -> service.enterRoom(
                userName,
                t_lobbyName.getText(),
                t_lobbyPassword.getText(),
                selectedCharacter,
                playmode
        ));
        topPanel.add(b_enterLobby, gbc);

        gbc.gridy = 5;
        b_logout = new JButton("로그아웃");
        topPanel.add(b_logout, gbc);
        b_logout.addActionListener(e -> service.disconnect(userName));

        panel.add(topPanel);
        panel.add(bottomPanel);

        return panel;
    }

    private JPanel createUserPanel(String userName) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());

         //UserName 필드
        JPanel usernamePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        usernamePanel.add(new JLabel("UserName: "+userName));
//        t_userName = new JTextField(10);
//        usernamePanel.add(t_userName);

        // Blue/Red 라벨 패널
        JPanel blueRedPanel = new JPanel(new GridLayout(1, 2));
        blueRedPanel.add(new JLabel("Blue", SwingConstants.CENTER));
        blueRedPanel.add(new JLabel("Red", SwingConstants.CENTER));

        topPanel.add(usernamePanel, BorderLayout.NORTH);
        topPanel.add(blueRedPanel, BorderLayout.CENTER);

        // 상단 패널을 UserPanel의 NORTH에 추가
        panel.add(topPanel, BorderLayout.NORTH);

        // 캐릭터 선택 패널
        JPanel characterPanel = new JPanel(new GridLayout(1, 2));
        characterPanel.setBorder(BorderFactory.createTitledBorder("캐릭터 선택"));

        // Blue 캐릭터 선택
        JPanel bluePanel = new JPanel(new GridLayout(3, 1));
        bluePanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));

        JButton blueCharacter1 = createCharacterButton("./src/image/character/water.png", "water");
        JButton blueCharacter2 = createCharacterButton("./src/image/character/wade.png", "wade");
        JButton blueCharacter3 = createCharacterButton("./src/image/character/blue3.png", "blue3");

        bluePanel.add(blueCharacter1);
        bluePanel.add(blueCharacter2);
        bluePanel.add(blueCharacter3);

        // Red 캐릭터 선택
        JPanel redPanel = new JPanel(new GridLayout(3, 1));
        redPanel.setBorder(BorderFactory.createLineBorder(Color.RED));

        JButton redCharacter1 = createCharacterButton("./src/image/character/fire.png", "fire");
        JButton redCharacter2 = createCharacterButton("./src/image/character/ember.png", "ember");
        JButton redCharacter3 = createCharacterButton("./src/image/character/red3.png", "red3");

        redPanel.add(redCharacter1);
        redPanel.add(redCharacter2);
        redPanel.add(redCharacter3);

        characterPanel.add(bluePanel);
        characterPanel.add(redPanel);
        panel.add(characterPanel, BorderLayout.CENTER);

        return panel;
    }

    // 버튼을 눌러 선택된 캐릭터의 이름 가져오기
    private JButton createCharacterButton(String imagePath, String characterName) {
        JButton button = new JButton();
        button.setActionCommand(characterName); // 캐릭터 이름 설정

        // 버튼 크기
        int buttonWidth = 100;
        int buttonHeight = 100;

        // 이미지 로드 및 크기 조정
        ImageIcon originalIcon = new ImageIcon(imagePath);
        Image scaledImage = originalIcon.getImage().getScaledInstance(
                buttonWidth, buttonHeight, Image.SCALE_SMOOTH // 부드러운 스케일링
        );
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        button.setIcon(scaledIcon); // 버튼에 이미지 설정
        button.setPreferredSize(new Dimension(buttonWidth, buttonHeight)); // 버튼 크기 설정

        // 버튼 클릭 이벤트
        button.addActionListener(e -> {
            System.out.println("선택된 캐릭터: " + e.getActionCommand());
            selectedCharacter = e.getActionCommand(); // 선택된 캐릭터 이름 저장
        });

        return button;
    }

    public void printLobbyList(String lobbyName){
        int len = t_lobbyList.getDocument().getLength();

        try {
            document.insertString(len, lobbyName + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        t_lobbyList.setCaretPosition(len);
    }

   
}
