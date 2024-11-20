package client;

import data.ChatMsg;
import data.GameMsg;

import java.io.*;
import java.net.Socket;

public class GameClientService {
    private GameClientTest gameClientTest;
    private Socket socket;
    private ObjectOutputStream out;
    private Thread receiveThread;

    private String serverAddress;
    private int serverPort;

    public GameClientService(GameClientTest gameClientTest, String serverAddress, int serverPort) {
        this.gameClientTest = gameClientTest;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    // 서버로 연결 요청
    public void connectToServer(String nickName, String roomName, String characterName, int mode, int team) {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.flush();

            // 메시지 수신 스레드
            receiveThread = new Thread(this::receiveMessages);
            receiveThread.start();

            // 서버로 LOGIN 코드 전송
            sendLOGIN(nickName, roomName, characterName, mode, team);

            gameClientTest.printDisplay("서버 소켓 생성");
        } catch (IOException e) {
            gameClientTest.printDisplay("서버 연결 실패: " + e.getMessage());
        }
    }

    // 로그아웃 & 서버와 연결 끊기
    public void disconnect(String roomName, String nickName, String characterName, int mode, int team) {
        try {
            if (socket != null && !socket.isClosed()) {
                ChatMsg chatMsg = new ChatMsg.Builder("LOGOUT")
                        .room(roomName)
                        .nickname(nickName)
                        .character(characterName)
                        .gameMode(mode)
                        .team(team)
                        .build();

                send(chatMsg); // 서버로 LOGOUT 코드 전송
                socket.close();
            }
        } catch (IOException e) {
            gameClientTest.printDisplay("연결 종료 중 오류: " + e.getMessage());
        } finally {
            receiveThread = null;
        }
    }

    // 서버로부터 받은 메시지
    private void receiveMessages() {
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()))) {
            while (Thread.currentThread() == receiveThread) {
                Object obj = in.readObject();

                // 객체 타입에 따라 다른 처리
                if (obj instanceof ChatMsg chatMsg) {
                    handleChatMessage(chatMsg);
                } else if (obj instanceof GameMsg gameMsg) {
                    handleGameMessage(gameMsg);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            gameClientTest.printDisplay("서버와의 연결이 종료되었습니다.");
        }
    }

    // 서버로부터 받은 ChatMsg 객체
    private void handleChatMessage(ChatMsg msg) {
        switch (msg.getCode()) {
            case "START_GAME" -> startGame(msg); // 게임 시작
            case "WAITING" -> gameClientTest.printDisplay("게임 시작 대기 중입니다..."); // 게임 대기
            case "TX_STRING" -> gameClientTest.printDisplay(msg.getNickname() + ": " + msg.getMessage()); // 텍스트 채팅 메시지
            case "TX_FILE" -> gameClientTest.printDisplay("파일 수신: " + msg.getMessage()); // 파일 채팅 메시지
            case "TX_IMAGE" -> { // 이미지 채팅 메시지
                gameClientTest.printDisplay("이미지 수신: " + msg.getMessage());
                gameClientTest.printDisplay(msg.getImage());
            }
            default -> gameClientTest.printDisplay("알 수 없는 메시지 유형 수신: " + msg.getCode());
        }
    }

    // 서버로부터 받은 GameMsg 객체
    private void handleGameMessage(GameMsg gameMsg) {
        // 게임 메시지 처리 로직
        gameClientTest.getGamePanel().updateOtherPlayerPosition(
                gameMsg.getNickname(),
                gameMsg.getCharacter(),
                gameMsg.getX(),
                gameMsg.getY()
        );
    }

    // 서버로 ChatMsg 타입 객체 전송
    public void send(ChatMsg msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            gameClientTest.printDisplay("메시지 전송 실패: " + e.getMessage());
        }
    }

    // 서버로 LOGIN 코드 전송
    private void sendLOGIN(String nickName, String roomName, String characterName, int mode, int team) {
        ChatMsg chatMsg = new ChatMsg.Builder("LOGIN")
                .nickname(nickName)
                .room(roomName)
                .character(characterName)
                .gameMode(mode)
                .team(team)
                .build();

        send(chatMsg);
    }

    // 게임 시작 요청
    public void requestStartGame(String nickName, String roomName, String characterName, int mode, int team) {
        ChatMsg chatMsg = new ChatMsg.Builder("JOIN_ROOM")
                .room(roomName)
                .nickname(nickName)
                .character(characterName)
                .gameMode(mode)
                .team(team)
                .build();

        send(chatMsg); // 서버로 메시지 전송
    }

    // 서버로부터 START_GAME 메시지 수신 시 게임 시작
    private void startGame(ChatMsg msg) {
        gameClientTest.startGamePanel(msg); // GameClient에서 GamePanel로 전환
    }

    public ObjectOutputStream getOutStream() {
        return out;
    }
}
