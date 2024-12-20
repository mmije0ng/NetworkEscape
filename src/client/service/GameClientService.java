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
    private String nickName;

    public GameClientService(GameClient gameClient, String serverAddress, int serverPort) {
        this.gameClient = gameClient;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

        setServerConfig();
    }

    private void setServerConfig(){
        String text = null;

        // server.txt에서 서버 설정 읽기
        try (BufferedReader br = new BufferedReader(new FileReader("server.txt"))) {
            serverAddress = br.readLine(); // 첫 번째 줄 ip 주소
            serverPort = Integer.parseInt(br.readLine()); // 두 번째 줄 포트 번호

            text = "클라이언트 시작, 서버 ip: " + serverAddress + ", 포트번호: " + serverPort;
            System.out.println(text);
        } catch (IOException e) {
            text = "server.txt 파일을 읽을 수 없어 기본 설정을 사용.\n클라이언트 시작, 서버 ip: " + serverAddress + ", 포트번호: " + serverPort;
            System.err.println(text);
        }
    }

    // 유저 로그인 - 로비 생성 화면으로 이동
    public void connectToServer(String serverAddress, int serverPort, String userName) {
        try {
            System.out.println("connectToServer : " + userName);
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
    public void disconnect(String nickName) {

        ChatMsg chatMsg = new ChatMsg.Builder("LOGOUT")
                .nickname(nickName)
                .build();
        send(chatMsg);  //서버로 LOGOUT 코드 전송

    }

    // 방 생성
    public void createRoom(String nickName, String roomName, String password, String characterName, int mode) {
        System.out.println("GameClientService createRoom characterName: " + characterName);
        if (!isPlayerSelected(characterName))
            return;

        sendCreate(nickName, roomName, password, characterName, mode, 1);
    }

    // 방 참가
    public void enterRoom(String nickName, String roomName, String password, String characterName, int mode) {
        System.out.println("GameClientService enterRoom characterName: " + characterName);
        if (!isPlayerSelected(characterName))
            return;

        sendEnter(nickName, roomName, password, characterName, mode);
    }

    // 캐릭터 선택 여부 체크
    private boolean isPlayerSelected(String characterName){
        if(characterName!=null)
            return true;

        JOptionPane.showMessageDialog(
                null,        // 부모 컴포넌트 (null일 경우 화면 중앙에 표시)
                "게임 캐릭터를 선택해 주세요",   // 메시지 내용
                "캐릭터 미선택",               // 창 제목
                JOptionPane.WARNING_MESSAGE // 경고 아이콘 사용
        );
        return false;
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
            case "LOGIN_FAIL" -> JOptionPane.showMessageDialog(
                    null,        // 부모 컴포넌트 (null일 경우 화면 중앙에 표시)
                    "사용자 이름 중복",   // 메시지 내용
                    "로그인 실패",               // 창 제목
                    JOptionPane.WARNING_MESSAGE // 경고 아이콘 사용
            );
            case "LOGOUT_SUCCESS" -> gameClient.startLoginPanel(serverAddress, serverPort);
            case "CREATE_SUCCESS" -> startRoom(msg);
            case "CREATE_FAIL" -> JOptionPane.showMessageDialog(
                    null,        // 부모 컴포넌트 (null일 경우 화면 중앙에 표시)
                    "방 정보를 입력해주세요",   // 메시지 내용
                    "방 생성 실패",               // 창 제목
                    JOptionPane.WARNING_MESSAGE // 경고 아이콘 사용
            );
            case "ENTER_SUCCESS" ->  {
                if(msg.getNickname().equals(nickName))
                    gameClient.startRoomPanel(this, msg);
                gameClient.printDisplay(msg.getTextMessage());
            }
            case "ENTER_FAIL" -> JOptionPane.showMessageDialog(
                    null,        // 부모 컴포넌트 (null일 경우 화면 중앙에 표시)
                    "대기방 입장에 실패하였습니다.",   // 메시지 내용
                    "방 입장 실패",               // 창 제목
                    JOptionPane.WARNING_MESSAGE // 경고 아이콘 사용
            );
            case "EXIT_SUCCESS" -> startMain(msg);
            case "START_GAME" -> startGame(msg); // 게임 시작
            case "UPDATE_ROOMLIST" -> gameClient.printRoom(msg);
            case "WAITING" -> gameClient.printDisplay("게임 시작 대기 중입니다..."); // 게임 대기
            case "TX_STRING" -> gameClient.getGameWithChatPanel().getChatPanel().printDisplay("[" + msg.getNickname() + "]: " + msg.getTextMessage()); // 텍스트 채팅 메시지
            case "TX_FILE" -> gameClient.getGameWithChatPanel().getChatPanel().printDisplay("[" + msg.getNickname() + "] 파일: " + msg.getFileName() + " (" + msg.getFileSize() + " bytes)"); // 파일 채팅 메시지
            case "TX_IMAGE" -> { // 이미지 채팅 메시지
                gameClient.getGameWithChatPanel().getChatPanel().printDisplay("[" + msg.getNickname() + "]: 이미지 " + msg.getFileName());
                gameClient.getGameWithChatPanel().getChatPanel().printDisplay(msg.getImage());
            }
            case "ALREADY_CHARACTER" -> { // 중복된 캐릭터 선택
                JOptionPane.showMessageDialog(
                        null,
                        "이미 선택된 캐릭터입니다. 다른 캐릭터를 선택하세요.",
                        "캐릭터 선택 중복 경고",
                        JOptionPane.WARNING_MESSAGE
                );
            }
//            default -> gameClient.printDisplay("수신 메시지 유형: " + msg.getCode());
        }
    }

    // 서버로부터 받은 GameMsg 객체
    private void handleGameMessage(GameMsg gameMsg) {

        switch (gameMsg.getCode()) {
            case "CREATE_ITEM" ->
                    gameClient.getGameWithChatPanel().getGamePanel().initializeItem(gameMsg.getItems());
            case "GET_POINT" ->
                    gameClient.getGameWithChatPanel().getGamePanel().updatePlayerPoint(gameMsg.getPoint());
            case "LIMIT_MOVE" -> gameClient.getGameWithChatPanel().getGamePanel().stopMove(gameMsg.getTeam());
            case "REMOVE_ITEM" -> gameClient.getGameWithChatPanel().getGamePanel().removeItem(gameMsg.getGotItem());
            case "NEXT_MAP" -> gameClient.startLoadingPanel(gameMsg, out);
            case "DOOR" -> gameClient.getGameWithChatPanel().getGamePanel().setCurrentDoorIndex(gameMsg.getCurrentDoorIndex());
            case "RESULT" -> gameClient.startResultPanel(gameMsg, out);
            default -> gameClient.getGameWithChatPanel().getGamePanel().updateOtherPlayerPosition(
                    gameMsg.getNickname(),
                    gameMsg.getCharacter(),
                    gameMsg.getX(),
                    gameMsg.getY()
            );
        }
    }

    // 서버로 ChatMsg 타입 객체 전송
    public void send (ChatMsg msg){
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            gameClient.printDisplay("메시지 전송 실패: " + e.getMessage());
        }
    }

    // 서버로 CREATE 코드 전송
    private void sendCreate (String nickName, String roomName, String password, String characterName, int mode, int team){
        ChatMsg chatMsg = new ChatMsg.Builder("CREATE")
                .nickname(nickName)
                .roomName(roomName)
                .password(password)
                .character(characterName)
                .gameMode(mode)
                .team(team)
                .build();
        send(chatMsg);
    }

    private void sendEnter (String nickName, String roomName, String password, String characterName,int mode){
        ChatMsg chatMsg = new ChatMsg.Builder("ENTER")
                .nickname(nickName)
                .roomName(roomName)
                .password(password)
                .character(characterName)
                .gameMode(mode)
                .build();

        send(chatMsg);
    }

    private void sendLOGIN (String nickName){
        ChatMsg chatMsg = new ChatMsg.Builder("LOGIN")
                .nickname(nickName)
                .build();
        send(chatMsg);
    }

    // 게임 시작 요청
    public void requestStartGame (String roomName, String nickName, String characterName,int mode, int team){
        ChatMsg chatMsg = new ChatMsg.Builder("START_GAME")
                .nickname(nickName)
                .roomName(roomName)
                .character(characterName)
                .gameMode(mode)
                .team(team)
                .build();

        System.out.println("Client START_GAME 요청, roomName: " + roomName + ", gameMode: " + mode);

        send(chatMsg); // 서버로 메시지 전송
    }

    // 서버로부터 START_GAME 메시지 수신 시 게임 시작
    private void startGame (ChatMsg msg){
        System.out.println("게임 시작, 코드: " + msg.getCode() + " 캐릭터: " + msg.getCharacter());
        gameClient.startGameWithChatPanel(msg, out); // GameClient에서 GamePanel로 전환
    }

    //메인화면
    private void startMain (ChatMsg msg){
        System.out.println("메인화면 시작");
        nickName = msg.getNickname();
        gameClient.startMainPanel(this, msg);
    }

    //대기방 화면
    private void startRoom (ChatMsg msg){
        gameClient.startRoomPanel(this, msg);
        gameClient.printDisplay(msg.getTextMessage());
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }
}