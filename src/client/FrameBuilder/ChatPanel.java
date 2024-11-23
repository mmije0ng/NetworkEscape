package client.FrameBuilder;

import data.ChatMsg;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

// 채팅 패널
public class ChatPanel extends JPanel {
    private ObjectOutputStream out; // 서버와의 통신을 위한 출력 스트림

    private String roomName, userName, character;
    private Integer team, gameMode;

    private JTextPane t_display;
    private DefaultStyledDocument document;

    private JTextField t_input;
    private JButton b_send, b_select;

    public ChatPanel(String roomName, String userName, String character, Integer gameMode, Integer team, ObjectOutputStream out) {
        this.roomName = roomName;
        this.userName = userName;
        this.character = character;
        this.gameMode = gameMode;
        this.team = team;
        this.out = out;

        buildGUI();

        setVisible(true);
    }

    private void buildGUI() {
        setSize(300, 600);
        setLayout(new BorderLayout());

        // 각각의 패널을 추가
        add(createDisplayPanel(), BorderLayout.CENTER);  // 메시지 출력 창
        add(createInputPanel(), BorderLayout.SOUTH);     // 입력 및 버튼 창
    }

    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        document = new DefaultStyledDocument();
        t_display = new JTextPane(document);
        t_display.setEditable(false); // 편집 불가한 Jt_display

        panel.add(new JScrollPane(t_display), BorderLayout.CENTER); // 스크롤 바 설정

        return panel;
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(); // 전체 입력 영역 패널
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS)); // 세로로 쌓이는 레이아웃

        // 텍스트 입력 필드
        t_input = new JTextField(40);
        t_input.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("t_input Listener 실행");
                sendTextMessage(); // 서버로 메시지 전송
            }
        });

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // 버튼을 가로로 나란히 배치
        b_select = new JButton("선택하기");
        b_send = new JButton("보내기");

        // "선택하기" 버튼 클릭 이벤트
        b_select.addActionListener(new ActionListener() {
            JFileChooser chooser = new JFileChooser();

            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "JPG & GIF & PNG Images", // 파일 선택 창에 표시될 문자열
                        "jpg", "gif", "png"
                );

                chooser.setFileFilter(filter);

                int ret = chooser.showOpenDialog(ChatPanel.this);
                if (ret != JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(ChatPanel.this, "파일을 선택하지 않았습니다.");
                    return;
                }

                File selectedFile = chooser.getSelectedFile();
                t_input.setText(selectedFile.getAbsolutePath());

                // 선택된 파일의 확장자 확인
                String fileName = selectedFile.getName();
                String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

                // 이미지 파일 전송
                if (fileExtension.equals("jpg") || fileExtension.equals("gif") || fileExtension.equals("png")) {
                    sendImage();
                }
                // 일반 파일 전송
                else {
                    sendFile();
                }
            }
        });

        // "보내기" 버튼 클릭 이벤트
        b_send.addActionListener(e -> {
            String message = t_input.getText().trim();
            if (!message.isEmpty()) {
                // 문자열 전송
                sendTextMessage();
                System.out.println("보낸 메시지: " + message);
                t_input.setText(""); // 입력 필드 초기화
            }
        });

        // 버튼 추가
        buttonPanel.add(b_select);
        buttonPanel.add(b_send);

        // 패널 구성
        inputPanel.add(t_input); // 입력 필드를 위에 추가
        inputPanel.add(Box.createVerticalStrut(5)); // 입력 필드와 버튼 사이에 간격 추가
        inputPanel.add(buttonPanel); // 버튼 패널을 아래에 추가

        return inputPanel;
    }

    // 이미지 전송
    private void sendImage() {
        String fileName = t_input.getText().strip();
        if(fileName.isEmpty()) return;

        File file = new File(fileName);
        if(!file.exists()){
            printDisplay(">> 파일이 존재하지 않습니다: "+fileName);
            return;
        }

        ImageIcon icon = new ImageIcon(fileName);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] imageBytes = fis.readAllBytes(); // 파일 데이터를 바이트 배열로 읽기

            ChatMsg imgMsg = new ChatMsg.Builder("TX_IMAGE")
                    .roomName(roomName)
                    .gameMode(gameMode)
                    .team(team)
                    .nickname(userName)
                    .character(character)
                    .imageBytes(imageBytes) // 바이트 배열로 이미지 전송
                    .fileName(file.getName())
                    .fileSize(file.length())
                    .build();

            send(imgMsg); // 서버로 전송
            t_input.setText(""); // 입력 필드 초기화
        } catch (IOException e) {
            printDisplay(">> 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 파일 전송
    private void sendFile() {
        String fileName = t_input.getText().strip();
        if (fileName.isEmpty()) return;

        File file = new File(fileName);
        if (!file.exists()) {
            printDisplay(">> 파일이 존재하지 않습니다: " + fileName);
            return;
        }

        BufferedInputStream bis = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(file));

            // 파일 전송 시작 메시지, 파일명과 파일 크기 포함
            ChatMsg fileMsg = new ChatMsg.Builder("TX_FILE")
                    .roomName(roomName)
                    .gameMode(gameMode)
                    .team(team)
                    .nickname(userName)
                    .character(character)
                    .fileName(file.getName())
                    .fileSize(file.length())
                    .build(); // 빌더 패턴에서 객체 생성

            send(fileMsg);

            byte[] buffer = new byte[1024];
            int nRead;
            while ((nRead = bis.read(buffer)) != -1) {
                out.write(buffer, 0, nRead); // 파일 데이터를 전송
            }
            out.flush(); // 모든 데이터 전송 후 버퍼 비우기
            printDisplay("파일 전송 완료: " + file.getName());
            sendTextMessage();
            t_input.setText("");

        } catch (FileNotFoundException e) {
            printDisplay(">> 파일이 존재하지 않습니다: " + e.getMessage() + "\n");
        } catch (IOException e) {
            printDisplay(">> 파일을 읽을 수 없습니다: " + e.getMessage() + "\n");
        } finally {
            // 스트림을 안전하게 닫기
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    printDisplay(">> 파일을 닫을 수 없습니다: " + e.getMessage() + "\n");
                }
            }
        }
    }

    // 텍스트 메시지 전송
    private void sendTextMessage(){
        String textMessage = t_input.getText();
        if(textMessage.isEmpty()) return;

        ChatMsg stringMsg = new ChatMsg.Builder("TX_STRING")
                .roomName(roomName)
                .gameMode(gameMode)
                .team(team)
                .nickname(userName)
                .character(character)
                .textMessage(textMessage)
                .build(); // 빌더 패턴에서 객체 생성

        send(stringMsg);

        t_input.setText("");
    }

    // 서버로 ChatMsg 객체 전송
    private void send(ChatMsg msg) {
        try{
            out.writeObject(msg);
//            out.reset(); // 참조 테이블 초기화
            out.flush();

        } catch (IOException e){
            e.printStackTrace();
            System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
        }
    }

    // 문자열 출력
    public void printDisplay(String msg) {
        int len = t_display.getDocument().getLength();
        try {
            document.insertString(len, msg + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        t_display.setCaretPosition(len);
    }

    // 이미지 출력
    public void printDisplay(ImageIcon icon) {
        t_display.setCaretPosition(t_display.getDocument().getLength());

        if (icon.getIconWidth() > 250) {
            Image img = icon.getImage();
            Image changeImg = img.getScaledInstance(250, -1, Image.SCALE_SMOOTH);
            icon = new ImageIcon(changeImg);
        }

        t_display.insertIcon(icon);
        printDisplay("");
        t_input.setText("");
    }

    public JTextField getT_input() {
        return t_input;
    }

}
