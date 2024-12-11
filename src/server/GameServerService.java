package server;

import data.BaseMsg;
import data.GameMsg;
import data.ChatMsg;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class GameServerService {
    private int port; // 포트 번호
    private ServerSocket serverSocket; // 서버 소켓
    private Thread acceptThread = null; // 클라이언트 요청 수락 스레드

    private Vector<ClientHandler> users = new Vector<>(); // 전체 참가자
    private List<String> rooms = new ArrayList<>();

    private Map<String, Vector<ClientHandler>> roomMap = new HashMap<>(); // 게임방 참가자 관리 맵
    private Map<String, Map<Integer, Integer>> teamCountMap = new HashMap<>(); // 팀별 인원 관리 맵, (팀, 합계)
    private Map<String, Map<Integer, Integer>> pointCountMap = new HashMap<>(); // 팀별 점수 관리 맵, (팀, 점수)
    private JTextArea t_display;

    public GameServerService(int port) {
        this.port = port;
    }

    public void setDisplayArea(JTextArea t_display) {
        this.t_display = t_display;
    }

    // 서버 시작 메서드
    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            printDisplay("서버가 시작되었습니다: " + InetAddress.getLocalHost().getHostAddress());

            acceptThread = new Thread(() -> {
                while (acceptThread == Thread.currentThread()) {
                    try {
                        if (serverSocket.isClosed()) break; // 서버 소켓이 닫힌 경우 스레드 종료
                        Socket clientSocket = serverSocket.accept();
                        printDisplay("클라이언트가 연결되었습니다: " + clientSocket.getInetAddress().getHostAddress());

                        ClientHandler clientHandler = new ClientHandler(clientSocket);
                        clientHandler.start();

                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) { // 서버 소켓이 닫히지 않았을 경우만 오류 출력
                            printDisplay("클라이언트 연결 중 오류 발생: " + e.getMessage());
                        }
                    }
                }
            });
            acceptThread.start();

        } catch (IOException e) {
            printDisplay("서버 시작 중 오류 발생: " + e.getMessage());
        }
    }

    // 게임 방 입장 클라이언트 추가
    private synchronized void addClientToRoom(String roomName, ClientHandler client) {
        roomMap.computeIfAbsent(roomName, k -> new Vector<>()).add(client); // 게임방에 클라이언트 추가
        teamCountMap.computeIfAbsent(roomName, k -> new HashMap<>()).merge(client.team, 1, Integer::sum); // 선택된 팀에 클라이언트 추가

        System.out.println("client team: "+client.team);

        printDisplay("[" + roomName + "] 참가자 수: " + roomMap.get(roomName).size());
    }

    // 퇴장 시 게임방에서 클라이언트 삭제
    private synchronized void removeClientFromRoom(String roomName, ClientHandler client) {
        Vector<ClientHandler> clients = roomMap.get(roomName);
        if (clients != null) {
            clients.remove(client);
            if (clients.isEmpty()) {
                roomMap.remove(roomName);
                teamCountMap.remove(roomName);
            } else {
                teamCountMap.get(roomName).merge(client.team, -1, Integer::sum);
            }
        }

    }
    //방 생성 시 모든 유저에게 알림
    private synchronized void broadcastAllUpdatedRoom(List<String> rooms, int gameMode){
        if(rooms.isEmpty()){
            for(ClientHandler user : users){
                user.send(new ChatMsg.Builder("UPDATE_ROOMLIST")
                        .gameMode(gameMode)
                        .build()
                );
            }
            return;
        }

        for(ClientHandler user : users){
            user.send(new ChatMsg.Builder("UPDATE_ROOMLIST")
                    .roomList(rooms)
                    .gameMode(gameMode)
                    .build()
            );
        }

    }

    // 같은 방의 유저들에게 ChatMsg 브로드캐스트
    private synchronized void broadcastToRoom(String roomName, ChatMsg chatMsg) {
        Vector<ClientHandler> clients = roomMap.get(roomName);
        if (clients == null) return; // 방이 존재하지 않으면 종료

        for (ClientHandler client : clients) {
            client.send(chatMsg);
        }
    }

    // 2대2 모드 시 같은 방, 같은 팀 유저에게만 ChatMsg 브로드캐스트
    private synchronized void broadcastToRoomForChat(String roomName, ChatMsg chatMsg) {
        Vector<ClientHandler> clients = roomMap.get(roomName);
        if (clients == null) return; // 방이 존재하지 않으면 종료

        for (ClientHandler client : clients) {
            // 2대2 모드인 경우 같은 팀에게만 메시지 전송
            if (chatMsg.getGameMode() != 2 || chatMsg.getTeam() == client.team) {
                client.send(chatMsg);
            }
        }
    }

    // 같은 방의 유저들에게 GameMsg 브로드캐스트
    private synchronized void broadcastToRoom(String roomName, GameMsg gameMsg) {
        Vector<ClientHandler> clients = roomMap.get(roomName);
        if (clients != null) {
            for (ClientHandler client : clients) {
                client.send(gameMsg);
            }
        }
    }

    // 서버 화면에 출력
    private void printDisplay(String msg) {
        if (t_display != null) {
            t_display.append(msg + "\n");
            t_display.setCaretPosition(t_display.getDocument().getLength());
        }
    }

    // 서버 연결 해제
    void serverDisconnect() {
        /* 소켓 닫기 */
        try{
            acceptThread = null;
            if(serverSocket!=null) {
                serverSocket.close();
                printDisplay("서버 종료");
                System.exit(-1);
            }

        } catch (IOException e){
            System.err.println("서버 닫기 오류> "+e.getMessage());
        }
    }

    // 클라인터트 핸들러
    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out; // 출력 객체
        private ObjectInputStream in; // 입력 객체
        private String roomName; // 방 이름
        private String password; //방 비밀번호
        private String nickName; // 닉네임
        private int team; // 팀 (1,2)
        private int point;  //포인트
        private int gameMode; // 게임모드 (1대1 모드이면 1, 2대2 모드이면 2)
        private String characterName; // 선택한 캐릭터 이름

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.out=null;
            this.in=null;
            this.roomName="";
            this.password="";
            this.nickName="";
            this.team=1;
            this.gameMode=1;
            this.characterName="";
            this.point=0;
        }

        @Override
        public void run() {
            receiveMessages();
        }

        // 클라이언트로부터 받은 메시지 관리
        private synchronized void receiveMessages() {
            try {
                in = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                out = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
                out.flush(); // 반드시 flush 호출

                BaseMsg msg;

                while ((msg = (BaseMsg) in.readObject()) != null) {
                    // 채팅 메시지 & 게임 시작 전 메시지
                    if (msg instanceof ChatMsg chatMsg) {
                        switch (chatMsg.getCode()) {
                            case "LOGIN" -> handleLOGIN(chatMsg); //로그인
                            case "LOGOUT" -> handleLOGOUT(chatMsg); //로그아웃
                            case "CREATE" -> handleCREATE(chatMsg); // 게임방 생성
                            case "ENTER" -> handleENTER(chatMsg);   //대기방 입장
                            case "EXIT" -> handleEXIT(chatMsg); // 대기방 퇴장
                            case "TX_STRING" -> handleTextMessage(chatMsg); // 스트링 메시지 (채팅)
                            case "TX_IMAGE" -> handleImageMessage(chatMsg); // 이미지 (채팅)
                            case "TX_FILE" -> handleFileMessage(chatMsg); // 파일 (채팅)
                            case "JOIN_ROOM" -> handleJoinRoom(chatMsg); // 게임방 입장
                            case "START_GAME" ->checkStartCondition(); // 게임 시작
                            default -> printDisplay("알 수 없는 메시지 유형: " + chatMsg.getCode());
                        }
                    }

                    // 게임 시작 이후 데이터
                    else if (msg instanceof GameMsg gameMsg) {
                        switch(gameMsg.getCode()){
                            case "REQUEST_ITEM" -> handleITEM(gameMsg);
                            case "APPLY_ITEM" -> handleAPPLY(gameMsg);
                            case "NEXT_MAP" -> handleNEXTMAP(gameMsg);
                            default -> handleGameMsg(gameMsg);
                        }
                    }
                }
            } catch (ClassNotFoundException | ClassCastException | IOException e) {
                e.printStackTrace();
                printDisplay("서버 수신 오류: " + e.getMessage());
                System.err.println("서버 수신 오류: " + e.getMessage());
            } finally {
                // 유저 삭제
                removeClientFromRoom(roomName, this);
                users.remove(this);
                closeResources();
            }
        }
        private void handleLOGIN(ChatMsg msg){
           nickName = msg.getNickname();

           //닉네임 중복 확인
           for(ClientHandler user : users){
               if(user.nickName.equals(msg.getNickname())){
                   printDisplay("닉네임 중복: "+user.nickName);
                   send(new ChatMsg.Builder("LOGIN_FAIL")
                           .nickname(nickName)
                           .build()
                   );
                   users.remove(this);
                   return;
               }
           }

           //로그인 성공 메시지
           send(new ChatMsg.Builder("LOGIN_SUCCESS")
                    .nickname(nickName)
                    .build()
           );

           //유저 추가
            synchronized (users) {
                users.add(this);
            }
            printDisplay("현재 참가자 수: " + users.size());
            System.out.println("닉네임: "+nickName);
        }

        private void handleLOGOUT(ChatMsg msg){
            //메시지로 전달받은 이름의 유저를 제거
            for(ClientHandler user: users){
                if(user.nickName.equals(msg.getNickname())){
                    users.remove(user);
                    break;
                }
            }
            send(new ChatMsg.Builder("LOGOUT_SUCCESS")
                    .build()
            );

            System.out.println(nickName + ": 로그아웃");
            printDisplay("현재 참가자 수: "+users.size());
        }

        //방 생성
        private void handleCREATE(ChatMsg msg) {
            nickName = msg.getNickname();
            roomName = msg.getRoomName();
            password = msg.getPassword();
            gameMode = msg.getGameMode();
            characterName = msg.getCharacter();
            team = msg.getTeam();

            //roomName, password, gameMode 입력하지 않으면 방 생성 실패
            if(roomName.equals("")||password.equals("")||gameMode==0){
                System.out.println("방 정보를 입력하세요.");
                send(new ChatMsg.Builder("CREATE_FAIL")
                        .roomName(roomName)
                        .build());
                return;
            }

            //같은 이름의 방이 있으면 방을 생성하지 못하도록
            for(String room : rooms){
                if(room.equals(msg.getRoomName())){
                    System.out.println("이미 존재하는 방 이름");
                    send(new ChatMsg.Builder("CREATE_FAIL")
                            .roomName(roomName)
                            .build());
                    return;
                }
            }

            //같은 이름의 방이 없으면 방 생성
            addClientToRoom(roomName, this); // 같은 이름의 게임방에 클라이언트 충가
            send(new ChatMsg.Builder("CREATE_SUCCESS")
                    .nickname(nickName)
                    .gameMode(gameMode)
                    .character(characterName)
                    .roomName(roomName)
                    .password(password)
                    .team(team)
                    .textMessage("["+msg.getRoomName()+"] 의 새로운 참가자 입장, 닉네임: "+msg.getNickname()+", 캐릭터: "+msg.getCharacter())
                    .build()
            );
            rooms.add(roomName);

            //방 생성을 모든 유저에게 알림
            broadcastAllUpdatedRoom(rooms, gameMode);

//            checkStartCondition(); // 게임이 시작 가능한지 체크
            System.out.println("닉네임: "+ nickName +", 캐릭터: "+characterName);
            printDisplay("[" + roomName + "] 방 생성자: " + nickName + " (팀: " + team + ")");
        }

        private void handleENTER(ChatMsg msg){
            nickName = msg.getNickname();
            roomName = msg.getRoomName();
            password = msg.getPassword();
            characterName = msg.getCharacter();
            gameMode = msg.getGameMode();

            System.out.println("handleEnter characterName: "+characterName);

            Vector<ClientHandler> roomUsers = roomMap.get(roomName);

            for(ClientHandler user : users){
                //방 이름이 빈 문자열이면 x
                if(msg.getRoomName().equals("")) return;

                //입력한 방이름, 비밀번호 검사
                if(user!=this&&user.roomName.equals(msg.getRoomName())&&user.password.equals(msg.getPassword())){
                    if((roomUsers.size()+1)%2!=0) team=1; //홀수번째 입장:1팀, 짝수번째 입장:2팀
                    else team=2;

                    // 캐릭터 선택 중복 검사
                    if (isCharacterAlready(msg.getCharacter())){

                    }


                    addClientToRoom(roomName, this); // 같은 이름의 게임방에 클라이언트 추가

                    printDisplay("새로운 참가자 enter");
                    printDisplay("roomName: "+roomName+", "+", nickName: "+nickName+", gameMode: "+gameMode+", team: "+team);

                    ChatMsg chatMsg = new ChatMsg.Builder("ENTER_SUCCESS")
                            .nickname(nickName)
                            .gameMode(user.gameMode)    //생성한 방의 게임모드
                            .character(characterName)
                            .roomName(roomName)
                            .team(team)
                            .password(password)
                            .textMessage("["+msg.getRoomName()+"] 의 새로운 참가자 입장, 닉네임: "+msg.getNickname()+" 캐릭터: "+msg.getCharacter())
                            .build();

                    send(chatMsg);

                    chatMsg.setCode("ENTER_OTHER_SUCCESS");
                    chatMsg.setTextMessage(printAllRoomPlayers(roomName));
                    broadcastToRoom(roomName, chatMsg);

                    return;
                }
            }

            send(new ChatMsg.Builder("ENTER_FAIL")
                    .build());

        }

        // 대기방 나가기
        private void handleEXIT(ChatMsg msg) {

            removeClientFromRoom(roomName,this);
            send(new ChatMsg.Builder("EXIT_SUCCESS")
                    .nickname(nickName)
                    .build()
            );
            Vector<ClientHandler> roomUsers = roomMap.get(roomName);
            if(roomUsers == null){
                rooms.remove(roomName);
            }

            broadcastAllUpdatedRoom(rooms, gameMode);
            printDisplay("[" + roomName + "] " + nickName + " 퇴장");
        }

        // LOGOUT 로그아웃
//        private void handleEXIT(ChatMsg msg) {
//            printDisplay("[" + roomName + "] " + nickName + " 로그아웃");
//        }

        // 클라이언트로부터 받은 텍스트 메시지 반향
        // code: TX_STRING
        private void handleTextMessage(ChatMsg msg) {
            String message = "[" + roomName + "] " + nickName + ": " + msg.getTextMessage();
            printDisplay(message);

            // 2대2 모드시 같은 팀 유저에게만 브로드캐스트
            broadcastToRoomForChat(msg.getRoomName(), msg);
        }

        // 클라이언트로부터 받은 이미지 반향
        // code: TX_IMAGE
        private void handleImageMessage(ChatMsg msg) {
            byte[] imageBytes = msg.getImageBytes();
            if (imageBytes == null) {
                printDisplay(">> 이미지 데이터가 없습니다: " + msg.getFileName());
                return;
            }

            ImageIcon imageIcon = new ImageIcon(imageBytes); // 바이트 배열을 ImageIcon으로 변환
            printDisplay("[" + msg.getRoomName() + "] " + msg.getNickname() + " (이미지 전송): " + msg.getFileName());

            msg.setImage(imageIcon);
            System.out.println("서버 imageIcon: "+imageIcon.getImage().toString());


            // 2대2 모드시 같은 팀 유저에게만 브로드캐스트
            broadcastToRoomForChat(msg.getRoomName(), msg);
        }

        // 클라이언트로부터 받은 파일 반향
        // code: TX_FILE
        private void handleFileMessage(ChatMsg msg) {
            String fileName = msg.getFileName();
            printDisplay("서버 파일 수신 시작: " + fileName + " (" + msg.getFileSize() + " bytes)");

            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileName))) {
                byte[] buffer = new byte[1024];
                long remaining = msg.getFileSize();
                int nRead;

                while (remaining > 0 && (nRead = in.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    bos.write(buffer, 0, nRead);
                    remaining -= nRead;
                }
                bos.flush();
                printDisplay("서버 파일 수신 완료: " + fileName);

                // 2대2 모드시 같은 팀 유저에게만 브로드캐스트
                broadcastToRoomForChat(msg.getRoomName(), msg);
            } catch (IOException e) {
                printDisplay("파일 저장 오류: " + e.getMessage());
            }
        }

//      게임 대기방 입장
        private void handleJoinRoom(ChatMsg msg) {
//            roomName = msg.getRoomName();
//            gameMode = msg.getGameMode();
//            team = msg.getTeam();
//            characterName = msg.getNickname();
//            nickName = msg.getNickname();

//            addClientToRoom(roomName, this); // 같은 이름의 게임방에 클라이언트 충가

            System.out.println("handleJoinRoom, mode: "+msg.getGameMode());

            checkStartCondition(); // 게임이 시작 가능한지 체크
        }

        //item 생성 요청 처리
        private void handleITEM(GameMsg msg){
            List<int[]> items = new ArrayList<>();
            // 문 위치에는 아이템이 생성되지 않도록 필터링
            List<int[]> blocks = msg.getBlocks().stream()
                    .filter(block -> block[0] <= 620 && block[1] >= 105)
                    .collect(Collectors.toList());

            for(int i=0; i<5; i++){
                int index = ThreadLocalRandom.current().nextInt(0, blocks.size());  //랜덤으로 한 블록 위에 생성되도록
                int[] random_block=blocks.get(index);
                int nx = random_block[0];   //랜덤 블록의 x좌표
                int ny = random_block[1]-random_block[3];    //랜덤 블록의 y좌표 +(블록 세로 사이즈);
                int type = ThreadLocalRandom.current().nextInt(1, 3); // 아이템 종류 (1 또는 2)
                items.add(new int[]{nx,ny,type});
            }
            GameMsg gameMsg = new GameMsg.Builder("CREATE_ITEM")
                    .roomName(msg.getRoomName())
                    .items(items)
                    .build();
            System.out.println("아이템 생성됨");
            broadcastToRoom(msg.getRoomName(),gameMsg);
        }

        //아이템 효과 적용 함수
        private void handleAPPLY(GameMsg msg){
            Vector<ClientHandler> roomUsers = roomMap.get(msg.getRoomName()); // 같은 방 유저들
            int userTeam = msg.getTeam();   //아이템을 먹은 플레이어의 팀
            for(ClientHandler user : roomUsers) {
                switch (msg.getGotItem()[2]) {  //item의 타입
                    case 1 -> getPoint(user,userTeam);
                    case 2 -> limitMove(user,userTeam);
                    default -> System.out.println("아이템 타입 오류");
                }
            }
            //아이템 적용 후 제거 메시지
            GameMsg gameMsg = new GameMsg.Builder("REMOVE_ITEM")
                    .roomName(roomName)
                    .team(team)
                    .gotItem(msg.getGotItem())
                    .build();
            broadcastToRoom(roomName,gameMsg);
        }
        //점수 얻기
        private void getPoint(ClientHandler user,int team){
            //같은 팀이면 점수 얻기
            if(user.team == team){
                user.point += 10;
                printDisplay("["+roomName+"] team: "+ this.team + "팀 점수 획득! 현재 점수: "+this.point);
                GameMsg gameMsg = new GameMsg.Builder("GET_POINT")
                        .roomName(roomName)
                        .team(user.team)
                        .point(10)  //10점
                        .build();
                user.send(gameMsg);

                // 점수 관리 맵 업데이트
                pointCountMap.computeIfAbsent(roomName, k -> new HashMap<>())
                        .merge(team, user.point, (existingValue, newValue) -> user.point); // 팀 점수를 user.point로 갱신
            }
        }

        //움직임 제한하기
        private void limitMove(ClientHandler user, int team){
            if(user.team != team){
                printDisplay("["+roomName+"] team: "+ this.team + "팀 움직임 제한: ");
                GameMsg gameMsg = new GameMsg.Builder("LIMIT_MOVE")
                        .roomName(roomName)
                        .team(team)
                        .build();
                user.send(gameMsg);
            }
        }

        // 게임 시작이 가능한지 체크
        private void checkStartCondition() {
            Vector<ClientHandler> roomUsers = roomMap.get(roomName); // 같은 방 유저들
            Map<Integer, Integer> teamCounts = teamCountMap.get(roomName); // 같은 방, 같은 팀 유저들

            printDisplay("["+roomName+"] gameMode: "+gameMode+" 게임 시작 가능 여부 체크");

            System.out.println("checkStartCondition");
            System.out.println("["+roomName+"] gameMode: "+gameMode+", team: "+team);
            printDisplay("["+roomName+"] gameMode: "+gameMode+", team: "+team);

            if (gameMode == 1) { // 1대1 모드: 같은 방에 두 명이 있으면 게임 시작
                if (roomUsers.size() == 2) { // 1대1 모드 시 같은 방의 유저가 두 명이면 게임 시작
                    startGameForRoom(roomUsers);
                } else {
                    sendWaitingMessage(); // 게임 대기
                }

            } else if (gameMode == 2) { // 2대2 모드: 팀별로 두 명씩 있으면 게임 시작
                System.out.println("team1 count: "+teamCounts.get(1));
                System.out.println("team2 count: "+teamCounts.get(2));

                if (teamCounts.get(1) == 2 && teamCounts.get(2) == 2) {
                    startGameForRoom(roomUsers);
                } else {
                    sendWaitingMessage();  // 게임 대기
                }
            }
        }

        // 대기방 입장 후 게임이 시작할 수 있는 상태가 아니면 클라이언트에게 대기 메시지 반향
        // code: WAITING
        private void sendWaitingMessage() {
            try {
                // ChatMsg 객체 생성
                ChatMsg chatMsg = new ChatMsg.Builder("WAITING")
                        .roomName(roomName)
                        .gameMode(gameMode)
                        .team(team)
                        .nickname(nickName)
                        .character(characterName)
                        .build(); // 빌더 패턴에서 객체 생성

                System.out.println("WAITING");

                out.writeObject(chatMsg);
                out.flush();
            } catch (IOException e) {
                printDisplay("대기 메시지 전송 오류: " + e.getMessage());
            }
        }


        // 게임이 가능한 모든 유저들에게 게임 시작 메시지 전송
        // code: START_GAME
        private void startGameForRoom(Vector<ClientHandler> roomUsers) {
            for (ClientHandler user : roomUsers) {
                try {

                    // ChatMsg 객체 생성
                    ChatMsg chatMsg = new ChatMsg.Builder("START_GAME")
                            .roomName(user.roomName)
                            .gameMode(user.gameMode)
                            .team(user.team)
                            .nickname(user.nickName)
                            .character(user.characterName)
                            .build(); // 빌더 패턴에서 객체 생성

                    System.out.println("서버 startGameForRoom: " + chatMsg.getCharacter());

                    // 점수 관리 맵 업데이트
                    pointCountMap.computeIfAbsent(roomName, k -> new HashMap<>())
                            .merge(team, user.point, (existingValue, newValue) -> user.point); // 팀 점수를 user.point로 갱신

                    user.out.writeObject(chatMsg);
                    user.out.reset(); // 참조 테이블 초기화
                    user.out.flush();
                } catch (IOException e) {
                    printDisplay("START_GAME 게임 시작 메시지 전송 오류: " + e.getMessage());
                }
            }
            printDisplay("[" + roomName + "] 게임 시작!");
        }

        // 클라이언트로부터 받은 GameMsg 객체를 같은 방의 유저들에게 전송
        // code: JUMP, MOVE, NEXT_MAP(level 3까지), DOOR
        private void handleGameMsg(GameMsg msg) {
            broadcastToRoom(msg.getRoomName(), msg);
        }

        // 다음 맵 이동
        private void handleNEXTMAP(GameMsg msg) {
            System.out.println("레벨: "+msg.getLevel());

            if(msg.getLevel()==4){ // 결과로 이동
                // 가장 높은 점수를 가진 팀 추출
                Map.Entry<Integer, Integer> highestTeam = getHighestScoringTeam(roomName);
                if (highestTeam != null) {
                    System.out.println("승리 팀: " + highestTeam.getKey() +
                            ", 점수: " + highestTeam.getValue());
                    printDisplay("["+roomName+"]의 승리 팀: " + highestTeam.getKey() +
                            ", 점수: " + highestTeam.getValue());
                } else {
                    System.out.println("점수 데이터가 없습니다.");
                }

                Vector<ClientHandler> roomUsers = roomMap.get(roomName); // 같은 방 유저들

                Map<String, String> winners  = new HashMap<>(); // 게임 승자 맵 (닉네임, 캐릭터)
                for(ClientHandler client: roomUsers){
                    if(client.team == highestTeam.getKey()){
                        System.out.println("게임 승자 추가: " + client.nickName + " -> " + client.characterName);
                        winners.put(client.nickName, client.characterName);
                    }
                }

                for(ClientHandler client: roomUsers){
                    GameMsg gameMsg = new GameMsg.Builder("RESULT")
                            .roomName(client.roomName)
                            .nickname(client.nickName)
                            .gameMode(client.gameMode)
                            .team(client.team)
                            .point(client.point)
                            .winTeam(highestTeam.getKey())
                            .winPoint(highestTeam.getValue())
                            .winners(winners)
                            .build();

                    client.send(gameMsg);
                }


            }

            else handleGameMsg(msg);
        }

        // 가장 높은 점수를 기록한 (팀 번호, 점수) 맵 가져오기
        private Map.Entry<Integer, Integer> getHighestScoringTeam(String roomName) {
            // 해당 roomName의 점수 맵 가져오기
            Map<Integer, Integer> teamScores = pointCountMap.getOrDefault(roomName, new HashMap<>());

            printDisplay("게임 결과");
            teamScores.forEach((team, score) -> printDisplay("Team: " + team + ", Score: " + score));


            // 점수 맵에서 가장 높은 점수를 가진 팀 찾기
            return teamScores.entrySet().stream()
                    .max(Map.Entry.comparingByValue()) // 값(점수)을 기준으로 최대값 찾기
                    .orElse(null); // 비어있는 경우 null 반환
        }

        // 같은 방 플레이어들의 목록 출력
        private String printAllRoomPlayers(String roomName) {
            // 방 정보 가져오기
            Vector<ClientHandler> roomUsers = roomMap.get(roomName);

            // 방이 존재하지 않거나 유저가 없는 경우 처리
            if (roomUsers == null || roomUsers.isEmpty()) {
                return "[" + roomName + "] 방에 플레이어가 없습니다.";
            }

            // StringBuilder로 플레이어 정보 생성
            StringBuilder playerInfo = new StringBuilder("[" + roomName + "] 의 플레이어 목록\n");
            for (ClientHandler client : roomUsers) {
                playerInfo.append("닉네임: ").append(client.nickName)
                        .append(", 캐릭터: ").append(client.characterName)
                        .append(", 팀 번호: ").append(client.team).append("\n");
            }

            // 콘솔 출력
            System.out.println(playerInfo);

            // 결과 반환
            return playerInfo.toString();
        }

        // 게임 캐릭터 중복 검사
        private Boolean isCharacterAlready(String characterName){
            // 방 정보 가져오기
            Vector<ClientHandler> roomUsers = roomMap.get(roomName);

            return roomUsers.stream()
                    .anyMatch(client -> client.characterName.equals(characterName));
        }

        // 서버 -> 클라이언트로 ChatMsg 전송
        private void send(ChatMsg msg) {
            try {
                out.writeObject(msg);
                out.reset(); // 참조 테이블 초기
                out.flush();
            } catch (IOException e) {
                printDisplay("ChatMsg 메시지 전송 실패: " + e.getMessage());
            }
        }

        // 서버 -> 클라이언트로 GameMsg 전송
        private void send(GameMsg msg) {
            try {
                out.writeObject(msg);
                out.reset(); // 참조 테이블 초기
                out.flush();
            } catch (IOException e) {
                printDisplay("GameMsg 메시지 전송 실패: " + e.getMessage());
            }
        }

        // 스트림, 클라이언트 소켓 닫기
        private void closeResources() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                printDisplay("자원 해제 실패: " + e.getMessage());
                System.err.println("자원 해제 실패: " + e.getMessage());
            }
        }
    }

}
