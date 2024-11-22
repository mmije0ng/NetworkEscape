package client.service;

import client.GameClient;
import data.ChatMsg;
import data.GameMsg;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class GameClientService {
    private GameClient gameClient;
    private Socket socket;
    private ObjectOutputStream out;
    private Thread receiveThread;

    private String serverAddress;
    private int serverPort;
    private String userName;

    public GameClientService(GameClient gameClient, String serverAddress, int serverPort) {
        this.gameClient = gameClient;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    // 서버로 연결 요청
    public void createRoom(String nickName, String roomName,String password, String characterName, int mode) {
        System.out.println("GameClientService createRoom characterName: "+characterName);

        if(characterName==null){
            JOptionPane.showMessageDialog(
                    null,        // 부모 컴포넌트 (null일 경우 화면 중앙에 표시)
                    "게임 캐릭터를 선택해 주세요",   // 메시지 내용
                    "캐릭터 미선택",               // 창 제목
                    JOptionPane.WARNING_MESSAGE // 경고 아이콘 사용
            );
            return;
        }

        // 서버로 LOGIN 코드 전송
        sendCreate(nickName, roomName,password, characterName, mode);
    }

    public void enterRoom(String nickName, String roomName, String password,String characterName, int mode){
        System.out.println("GameClientService enterRoom characterName: "+characterName);

        if(characterName==null){
            JOptionPane.showMessageDialog(
                    null,        // 부모 컴포넌트 (null일 경우 화면 중앙에 표시)
                    "게임 캐릭터를 선택해 주세요",   // 메시지 내용
                    "캐릭터 미선택",               // 창 제목
                    JOptionPane.WARNING_MESSAGE // 경고 아이콘 사용
            );
            return;
        }

        sendEnter(nickName,roomName,password,characterName, mode);
    }

    //유저 로그인 - 로비 생성 화면으로 이동
    public void connectToServer(String serverAddress,  int serverPort, String userName){
        try {
            System.out.println("connectToServer : "+userName);
            socket = new Socket(serverAddress, serverPort);
            out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.flush();

            receiveThread = new Thread(this::receiveMessages);
            receiveThread.start();

            sendLOGIN(userName);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }



    }
    // 유저 로그아웃 - 로그인 화면으로 이동
    public void disconnect(String nickName){
        try {
            ChatMsg chatMsg=new ChatMsg.Builder("LOGOUT")
                .nickname(nickName)
                .build();
            send(chatMsg);  //서버로 LOGOUT 코드 전송
            socket.close();

        } catch (IOException e) {
            gameClient.printDisplay("연결 종료 중 오류: " + e.getMessage());
        } finally {
            receiveThread = null;
        }
    }
    //대기방 나가기
    public void exitRoom(String roomName, String nickName, String characterName, int mode, int team) {
        ChatMsg chatMsg = new ChatMsg.Builder("EXIT")
                .roomName(roomName)
                .nickname(nickName)
                .character(characterName)
                .gameMode(mode)
                .team(team)
                .build();
        send(chatMsg); // 서버로 EXIT 코드 전송
    }

    // 로그아웃 & 서버와 연결 끊기
    public void exitGame(String roomName, String nickName, String characterName, int mode, int team) {
        try {
            if (socket != null && !socket.isClosed()) {
                ChatMsg chatMsg = new ChatMsg.Builder("EXIT")
                        .roomName(roomName)
                        .nickname(nickName)
                        .character(characterName)
                        .gameMode(mode)
                        .team(team)
                        .build();

                send(chatMsg); // 서버로 EXIT 코드 전송
                socket.close();
            }
        } catch (IOException e) {
            gameClient.printDisplay("연결 종료 중 오류: " + e.getMessage());
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
            gameClient.printDisplay("서버와의 연결이 종료되었습니다.");
        }
    }

    // 서버로부터 받은 ChatMsg 객체
    private void handleChatMessage(ChatMsg msg) {
        switch (msg.getCode()) {
            case "LOGIN_SUCCESS" -> startMain(msg);
//            case "LOGIN_FAIL" -> 닉네임이 중복된다는 알람
            case "CREATE_SUCCESS" -> startRoom(msg);
            case "CREATE_FAIL" -> System.out.println("방 생성 실패");
            case "ENTER_SUCCESS" -> startRoom(msg);
//            case "ENTER_FAIL" -> 입장 실패시 알람
            case "EXIT_SUCCESS" -> startMain(msg);
            case "START_GAME" -> startGame(msg); // 게임 시작
            case "UPDATE_ROOMLIST" -> gameClient.printRoom(msg);
            case "WAITING" -> gameClient.printDisplay("게임 시작 대기 중입니다..."); // 게임 대기
            case "TX_STRING" -> gameClient.printDisplay(msg.getNickname() + ": " + msg.getMessage()); // 텍스트 채팅 메시지
            case "TX_FILE" -> gameClient.printDisplay("파일 수신: " + msg.getMessage()); // 파일 채팅 메시지
            case "TX_IMAGE" -> { // 이미지 채팅 메시지
                gameClient.printDisplay("이미지 수신: " + msg.getMessage());
                gameClient.printDisplay(msg.getImage());
            }
            default -> gameClient.printDisplay("알 수 없는 메시지 유형 수신: " + msg.getCode());
        }
    }

    // 서버로부터 받은 GameMsg 객체
    private void handleGameMessage(GameMsg gameMsg) {
        // 게임 메시지 처리 로직
        gameClient.getGamePanel().updateOtherPlayerPosition(
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
            gameClient.printDisplay("메시지 전송 실패: " + e.getMessage());
        }
    }

    // 서버로 CREATE 코드 전송
    private void sendCreate(String nickName, String roomName,String password, String characterName, int mode) {
        ChatMsg chatMsg = new ChatMsg.Builder("CREATE")
                .nickname(nickName)
                .roomName(roomName)
                .password(password)
                .character(characterName)
                .gameMode(mode)
                .build();
        send(chatMsg);
    }
    private void sendEnter(String nickName, String roomName, String password, String characterName, int mode){
        ChatMsg chatMsg=new ChatMsg.Builder("ENTER")
                .nickname(nickName)
                .roomName(roomName)
                .password(password)
                .character(characterName)
                .gameMode(mode)
                .build();

        send(chatMsg);
    }

    private void sendLOGIN(String nickName){
        ChatMsg chatMsg = new ChatMsg.Builder("LOGIN")
                .nickname(nickName)
                .build();
        send(chatMsg);
    }

    // 게임 시작 요청
    public void requestStartGame(String nickName, String roomName, String characterName, int mode, int team) {
        ChatMsg chatMsg = new ChatMsg.Builder("JOIN_ROOM")
                .nickname(nickName)
                .roomName(roomName)
                .character(characterName)
                .gameMode(mode)
                .team(team)
                .build();

        System.out.println("JOIN_ROOM, roomName: "+roomName+", gameMode: "+mode);

        send(chatMsg); // 서버로 메시지 전송
    }

    // 서버로부터 START_GAME 메시지 수신 시 게임 시작
    private void startGame(ChatMsg msg) {
        System.out.println("게임 시작, 코드: "+msg.getCode() + " 캐릭터: "+msg.getCharacter());
        gameClient.startGamePanel(msg); // GameClient에서 GamePanel로 전환
    }

    private void startMain(ChatMsg msg){
        System.out.println("메인화면 시작");
        gameClient.startMainPanel(this,msg);
    }

    private void startRoom(ChatMsg msg){
        gameClient.startRoomPanel(this,msg);
        gameClient.printDisplay(msg.getRoomName()+"의 새로운 참가자 입장, 닉네임: "+msg.getNickname()+" 캐릭터: "+msg.getCharacter());
    }

    public ObjectOutputStream getOutStream() {
        return out;
    }
}