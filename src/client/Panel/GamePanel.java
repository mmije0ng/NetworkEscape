package client.Panel;

import data.GameMsg;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GamePanel extends JPanel {
    // 메시지 전송 스레드 관련 변수
    private Thread messageSenderThread; // 메시지 전송 스레드
    private volatile boolean running = true; // 스레드 실행 상태를 나타냄

    // 플레이어 관련 정보
    private String nickName; // 플레이어 이름
    private String character; // 플레이어 캐릭터 타입
    private String roomName; // 플레이어가 속한 방 이름
    private int playerX; // 플레이어의 X 좌표
    private int playerY = getHeight() - 40; // 플레이어의 Y 좌표
    private boolean isJumping = false; // 플레이어가 점프 중인지 여부

    // 키 입력 상태 관리
    // 현재 눌린 키 상태를 저장
    private Set<Integer> keysPressed = new HashSet<>();

    // 다른 플레이어 관련 정보
    private final Map<String, int[]> otherPlayers = new HashMap<>(); // 다른 플레이어 위치 {x, y}
    private final Map<String, String> otherPlayerCharacters = new HashMap<>(); // 다른 플레이어 캐릭터 타입

    // 서버와 통신
    private ObjectOutputStream out; // 서버와의 통신을 위한 출력 스트림

    // 이미지 리소스
    private Map<String, Image> characterImages = new HashMap<>(); // 캐릭터 이미지를 저장하는 맵
    private Image backgroundImage; // 배경 이미지
    private Image blockImage; // 블록 이미지

    private boolean isFalling = false; // 플레이어가 낙하 중인지 여부

    // 블록 위치 정보
    // 블록 리스트 {x, y, width, height}
    private Map<Integer, List<int[]>> blocks = new HashMap<>();

    private int remainingTime = 60; // 제한 시간 (초)

    private boolean isTimeRunning = true; // 타이머 실행 여부
    private Thread timerThread; // 타이머 스레드

    private Integer mode; // 게임 모드;
    private Integer team; // 팀
    private Integer level; // 게임 레벨

    private List<Image> doorImages = new ArrayList<>(); // 문 이미지 리스트
    private int doorX = 660; // 문 위치 X
    private int doorY = getHeight() - 498; // 문 위치 Y
    private boolean isDoorOpen = false; // 문 열림 상태

    private int currentDoorIndex = 0; // 현재 문 이미지 인덱스

    private boolean isBlocked = false; // 플레이어가 움직임이 차단되었는지 여부

    public GamePanel(String nickName, String character, String roomName, Integer mode, Integer team, Integer level, ObjectOutputStream out) {
        this.nickName = nickName;
        this.character = character;
        this.roomName = roomName;

        this.mode = mode;
        this.team = team;
        this.level = level;

        this.out = out;

        setSize(800, 600);

        loadImages(); // 이미지 로드
        setFocusable(true); // 키보드 입력 활성화

        // 주기적으로 포커스 유지
        Timer focusTimer = new Timer(100, e -> {
            if (!isFocusOwner()) {
                requestFocusInWindow();
            }
        });
        focusTimer.start();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                initializePlayerPosition(); // 화면 크기 변경 시 플레이어 초기 위치 재설정
                initializeBlocks(); // 블록 재설정
                
                // 문 위치 조정
                doorX = 660;
                doorY  =  getHeight() - 498;
                
                repaint();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isRelevantKey(e.getKeyCode())) {
                    keysPressed.add(e.getKeyCode());
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (isRelevantKey(e.getKeyCode())) {
                    keysPressed.remove(e.getKeyCode());
                }
            }
        });

        // 전역 키 이벤트 리스너 추가
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    keysPressed.add(e.getKeyCode());
                } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                    keysPressed.remove(e.getKeyCode());
                }
                return false;
            }
        });

        startMessageSenderThread(); // 메시지 전송 스레드 시작
        startTimerThread(); // 제한 시간 타이머 스레드 시작
        startDoorCheckTimer(); // 문이 열려있는지 닫혀있는지 체크
        startNextMapThread(); // 다음 맵으로 전환 가능한지 체크하는 스레드 시작

        System.out.println("현재 맵 레벨: "+ level);
    }

    // 다음 맵으로 초기화
    public void initializeNextMap(int level) {
        this.level = level; // 레벨 재설정

        loadImages(); // 새로운 맵 이미지 로드
        initializePlayerPosition(); // 플레이어 위치 초기화

        isDoorOpen = false; // 문 닫힘
        currentDoorIndex = 0; // 현재 문 인덱스 0으로 초기화
        isBlocked = false; // 플레이어 움직임 차단

        initializeBlocks(); // 블록 초기화

        // 제한 시간이 종료되어 타이머 스레드가 종료되었다면 다시 시작
        remainingTime = 60-level*10; // 제한 시간 초기화
        isTimeRunning = true;
        startTimerThread();
    }

    // 맵 초기화
    private void initializeBlocks() {
        blocks.clear(); // 기존 블록 제거

        List<int[]> level1Blocks = new ArrayList<>();

        // 하단 레벨
        level1Blocks.add(new int[]{220, getHeight() - 40, 40, 40});
        level1Blocks.add(new int[]{260, getHeight() - 40, 40, 40});
        level1Blocks.add(new int[]{350, getHeight() - 40, 40, 40});
        level1Blocks.add(new int[]{390, getHeight() - 40, 40, 40});
        level1Blocks.add(new int[]{430, getHeight() - 40, 40, 40});
        level1Blocks.add(new int[]{470, getHeight() - 40, 40, 40});
        level1Blocks.add(new int[]{510, getHeight() - 40, 40, 40});
        level1Blocks.add(new int[]{710, getHeight() - 40, 40, 40});
        level1Blocks.add(new int[]{750, getHeight() - 40, 40, 40});

        // 두 번째 레벨
        level1Blocks.add(new int[]{640, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{600, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{560, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{520, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{480, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{440, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{400, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{360, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{280, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{240, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{0, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{40, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{80, getHeight() - 120, 40, 40});
        level1Blocks.add(new int[]{120, getHeight() - 120, 40, 40});

        // 세 번째 레벨
        level1Blocks.add(new int[]{140, getHeight() - 180, 40, 20});
        level1Blocks.add(new int[]{180, getHeight() - 180, 40, 20});
        level1Blocks.add(new int[]{220, getHeight() - 180, 20, 20});
        level1Blocks.add(new int[]{290, getHeight() - 240, 50, 40});
        level1Blocks.add(new int[]{340, getHeight() - 240, 40, 40});
        level1Blocks.add(new int[]{380, getHeight() - 240, 40, 40});
        level1Blocks.add(new int[]{400, getHeight() - 240, 40, 40});
        level1Blocks.add(new int[]{440, getHeight() - 240, 40, 40});
        level1Blocks.add(new int[]{480, getHeight() - 240, 40, 40});
        level1Blocks.add(new int[]{570, getHeight() - 220, 40, 40});
        level1Blocks.add(new int[]{610, getHeight() - 220, 40, 40});
        level1Blocks.add(new int[]{650, getHeight() - 220, 40, 40});
        level1Blocks.add(new int[]{750, getHeight() - 240, 40, 40});

        // 네 번째 레벨
        level1Blocks.add(new int[]{590, getHeight() - 320, 40, 40});
        level1Blocks.add(new int[]{630, getHeight() - 320, 40, 40});
        level1Blocks.add(new int[]{670, getHeight() - 320, 40, 40});
        level1Blocks.add(new int[]{330, getHeight() - 340, 50, 40});
        level1Blocks.add(new int[]{380, getHeight() - 340, 40, 40});
        level1Blocks.add(new int[]{420, getHeight() - 340, 40, 40});
        level1Blocks.add(new int[]{460, getHeight() - 340, 40, 40});
        level1Blocks.add(new int[]{500, getHeight() - 340, 40, 40});

        level1Blocks.add(new int[]{220, getHeight() - 320, 50, 40});
        level1Blocks.add(new int[]{180, getHeight() - 320, 40, 40});
        level1Blocks.add(new int[]{140, getHeight() - 320, 40, 40});
        level1Blocks.add(new int[]{100, getHeight() - 320, 40, 40});
        level1Blocks.add(new int[]{60, getHeight() - 320, 40, 40});
        level1Blocks.add(new int[]{0, getHeight() - 350, 60, 70});

        // 다섯 번째 레벨
        level1Blocks.add(new int[]{100, getHeight() - 420, 40, 30});
        level1Blocks.add(new int[]{140, getHeight() - 440, 40, 40});

        // 가장 상단
        for (int i = 180; i <= getWidth(); i += 40) {
            level1Blocks.add(new int[]{i, getHeight() - 440, 40, 40});
        }

        blocks.put(1, level1Blocks);

        List<int[]> level2Blocks = new ArrayList<>();
        // 하단 레벨
        level2Blocks.add(new int[]{300, getHeight() - 40, 40, 40});
        level2Blocks.add(new int[]{340, getHeight() - 40, 40, 40});
        level2Blocks.add(new int[]{380, getHeight() - 40, 40, 40});
        level2Blocks.add(new int[]{420, getHeight() - 40, 40, 40});

        level2Blocks.add(new int[]{710, getHeight() - 40, 40, 40});
        level2Blocks.add(new int[]{750, getHeight() - 40, 40, 40});

        level2Blocks.add(new int[]{640, getHeight() - 120, 40, 40});
        level2Blocks.add(new int[]{600, getHeight() - 120, 40, 40});
        level2Blocks.add(new int[]{560, getHeight() - 120, 40, 40});
        level2Blocks.add(new int[]{520, getHeight() - 120, 40, 40});
        level2Blocks.add(new int[]{480, getHeight() - 120, 40, 40});
        level2Blocks.add(new int[]{400, getHeight() - 120, 40, 40});
        level2Blocks.add(new int[]{360, getHeight() - 120, 40, 40});
        level2Blocks.add(new int[]{280, getHeight() - 120, 40, 40});
        level2Blocks.add(new int[]{230, getHeight() - 120, 50, 40});
        level2Blocks.add(new int[]{80, getHeight() - 120, 40, 40});
        level2Blocks.add(new int[]{120, getHeight() - 120, 40, 40});

        level2Blocks.add(new int[]{0, getHeight() - 200, 40, 40});
        level2Blocks.add(new int[]{40, getHeight() - 200, 40, 40});
        level2Blocks.add(new int[]{130, getHeight() - 200, 40, 40});
        level2Blocks.add(new int[]{170, getHeight() - 200, 40, 40});

        level2Blocks.add(new int[]{260, getHeight() - 260, 60, 40});
        level2Blocks.add(new int[]{320, getHeight() - 260, 40, 40});
        level2Blocks.add(new int[]{360, getHeight() - 260, 40, 40});
        level2Blocks.add(new int[]{400, getHeight() - 260, 40, 40});
        level2Blocks.add(new int[]{440, getHeight() - 260, 40, 40});
        level2Blocks.add(new int[]{480, getHeight() - 260, 40, 40});
        level2Blocks.add(new int[]{520, getHeight() - 260, 40, 40});
        level2Blocks.add(new int[]{600, getHeight() - 260, 40, 40});
        level2Blocks.add(new int[]{640, getHeight() - 260, 40, 40});
        level2Blocks.add(new int[]{680, getHeight() - 260, 40, 40});
        level2Blocks.add(new int[]{720, getHeight() - 260, 60, 40});

        level2Blocks.add(new int[]{0, getHeight() - 340, 30, 40});
        level2Blocks.add(new int[]{30, getHeight() - 340, 40, 40});
        level2Blocks.add(new int[]{70, getHeight() - 340, 40, 40});
        level2Blocks.add(new int[]{110, getHeight() - 340, 40, 40});

        level2Blocks.add(new int[]{420, getHeight() - 340, 40, 20});
        level2Blocks.add(new int[]{460, getHeight() - 340, 40, 20});
        level2Blocks.add(new int[]{500, getHeight() - 340, 40, 20});
        level2Blocks.add(new int[]{540, getHeight() - 340, 40, 20});

        level2Blocks.add(new int[]{640, getHeight() - 320, 40, 20});
        level2Blocks.add(new int[]{680, getHeight() - 320, 40, 20});

        level2Blocks.add(new int[]{350, getHeight() - 360, 40, 20});
        level2Blocks.add(new int[]{310, getHeight() - 360, 40, 20});
        level2Blocks.add(new int[]{270, getHeight() - 360, 40, 20});
        level2Blocks.add(new int[]{230, getHeight() - 360, 40, 20});
        level2Blocks.add(new int[]{190, getHeight() - 360, 40, 20});
        level2Blocks.add(new int[]{150, getHeight() - 360, 40, 20});
        level2Blocks.add(new int[]{110, getHeight() - 360, 40, 20});
        level2Blocks.add(new int[]{80, getHeight() - 360, 30, 20});

        level2Blocks.add(new int[]{0, getHeight() - 380, 40, 40});
        level2Blocks.add(new int[]{40, getHeight() - 380, 40, 40});

        level2Blocks.add(new int[]{100, getHeight() - 460, 40, 40});

        // 가장 상단
        for (int i = 180; i <= getWidth(); i += 40) {
            level2Blocks.add(new int[]{i, getHeight() - 440, 40, 40});
        }

        blocks.put(2, level2Blocks);

    }

    // 게임에 필요한 이미지 로드
    private void loadImages() {
        try {
            // 문 이미지 로드
            doorImages.add(createImageIcon("/image/door/door1.png").getImage());
            doorImages.add(createImageIcon("/image/door/door2.png").getImage());
            doorImages.add(createImageIcon("/image/door/door3.png").getImage());
            doorImages.add(createImageIcon("/image/door/door4.png").getImage());

            // 캐릭터 이미지 로드
            characterImages.put("fire", createImageIcon("/image/character/fire.png").getImage());
            characterImages.put("water", createImageIcon("/image/character/water.png").getImage());
            characterImages.put("엠버", createImageIcon("/image/character/ember.png").getImage());
            characterImages.put("웨이드", createImageIcon("/image/character/wade.png").getImage());
            characterImages.put("버럭이", createImageIcon("/image/character/anger.png").getImage());
            characterImages.put("슬픔이", createImageIcon("/image/character/sad.png").getImage());

            // 배경 및 블록 이미지 로드
            backgroundImage = createImageIcon("/image/background/stage"+level+".png").getImage();
            blockImage = createImageIcon("/image/block/block2.png").getImage();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("이미지 파일 로드 실패");
        }
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

    // 입력된 키가 유효한 키인지 확인
    // 좌, 우, 스페이스바
    private boolean isRelevantKey(int keyCode) {
        return keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_SPACE;
    }

    // 키 입력 처리
    private void processKeys() {
        // 문이 열렸으면 키 입력을 무시
        if (isDoorOpen) {
            return;
        }

        int newX = playerX;

        // 좌우 이동
        if (keysPressed.contains(KeyEvent.VK_LEFT)) newX -= 2;
        if (keysPressed.contains(KeyEvent.VK_RIGHT)) newX += 2;

        // 블록과 충돌하지 않으면 이동
        if (!isCollidesWithBlock(newX, playerY)) {
            playerX = newX;
            sendPlayerPosition("MOVE", playerX, playerY);
        }

        if(isPlayerAtDoor()){
            return; // 움직임 차단 상태에서는 입력을 처리하지 않음
        }

        // 점프 처리
        if (keysPressed.contains(KeyEvent.VK_SPACE) && !isJumping) {
            startJump();
        }

        repaint();
    }

    // 플레이어 초기 위치 설정
    // 작동 안 됨.. 서버로 메시지 전송 해야 될 듯
    private void initializePlayerPosition() {
        playerX = ThreadLocalRandom.current().nextInt(5, 41); // 5에서 40 사이의 랜덤 값
        playerY = getHeight() - 40; // 기본 Y 좌표
//        int offset = 30; // 플레이어 간 간격
//        if (!otherPlayers.isEmpty()) {
//            playerX += otherPlayers.size() * offset;
//        }
    }

    // 플레이어가 블록 위에 서 있지 않은 경우 하강
    private void applyGravity() {
        if (!isFalling && !isJumping) {
            new Thread(() -> {
                try {
                    isFalling = true;
                    while (!isStandingOnBlock(playerX, playerY)) {
                        int newY = playerY + 2; // 낙하 속도
                        if (newY + 40 >= getHeight()) { // 화면 바닥에 닿으면 멈춤
                            playerY = getHeight() - 40; // 바닥 위로 위치 고정
                            break;
                        }
                        playerY = newY;

                        sendPlayerPosition("FALL", playerX, playerY); // 서버에 낙하 정보 전송
                        repaint();

                        processKeys(); // 중력 중에도 키 입력 처리
                        Thread.sleep(7); // 낙하 속도 조정 8
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    isFalling = false; // 낙하 상태 해제
                }
            }).start();
        }
    }

   // 플레이어가 블록 위에 서있는지 체크
    private boolean isStandingOnBlock(int x, int y) {
        List<int[]> levelBlocks = blocks.get(level);
        if (levelBlocks == null) return false; // 현재 레벨의 블록이 없다면 return false

        for (int[] block : levelBlocks) {
            int bx = block[0], by = block[1], bWidth = block[2];
            if (x + 40 > bx && x < bx + bWidth && y + 40 == by) {
                return true;
            }
        }
        return false;
    }
    
    // 플레이어 위치 정보 출력
    private void printPlayerPositions() {
        otherPlayers.forEach((name, position) ->
                System.out.printf("플레이어 %s 위치: X=%d, Y=%d%n", name, position[0], position[1]));
    }

    // 플레이어가 블록에 충돌했는지 체크
    private boolean isCollidesWithBlock(int x, int y) {
        List<int[]> levelBlocks = blocks.get(level);
        if (levelBlocks == null) return false; // 현재 레벨의 블록이 없다면 return false

        for (int[] block : levelBlocks) {
            int bx = block[0], by = block[1], bWidth = block[2], bHeight = block[3];
            if (x + 40 > bx && x < bx + bWidth && y + 40 > by && y < by + bHeight) {
                return true; // 충돌이 발생한 경우
            }
        }
        return false; // 충돌하지 않은 경우
    }

    // 점프 효과
    private void startJump() {
        if (isJumping) return;
        isJumping = true;
        int jumpHeight = 82;
        int jumpSpeed = 2;

        new Thread(() -> {
            try {

                // 상승 단계
                for (int i = 0; i < jumpHeight / jumpSpeed; i++) {
                    int newY = playerY - jumpSpeed;
                    if (!isCollidesWithBlock(playerX, newY)) {
                        playerY = newY;
                        
                        // 서버로 현재 플레이어의 위치와 JUMP 코드 전송
                        sendPlayerPosition("JUMP", playerX, playerY);
                        repaint();
                    }
                    Thread.sleep(5);
                }
                
                // 하강 단계
                for (int i = 0; i < jumpHeight / jumpSpeed; i++) {
                    int newY = playerY + jumpSpeed;
                    if (!isCollidesWithBlock(playerX, newY)) {
                        playerY = newY;
                        sendPlayerPosition("JUMP", playerX, playerY);
                        repaint();
                    }
                    Thread.sleep(5);
                }

                applyGravity(); // 점프 후 중력 적용
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                isJumping = false;
            }
        }).start();
    }

    // 서버로 메시지 전송 스레드
    private void startMessageSenderThread() {
        messageSenderThread = new Thread(() -> {
            while (running) {
                try {
                    processKeys(); // 키 입력 처리
                    applyGravity(); // 중력 적용
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        messageSenderThread.start();
    }

   // 서버로 플레이어의 액션(MOVE, JUMP, FALL), 위치 전송
    private synchronized void sendPlayerPosition(String action, int x, int y) {
        if (out != null) {
            try {
                GameMsg gameMsg = new GameMsg.Builder(action)
                        .roomName(roomName)
                        .gameMode(mode)
                        .team(team)
                        .nickname(nickName)
                        .character(character)
                        .x(x)
                        .y(y)
                        .build();

                out.writeObject(gameMsg);
                out.flush();
            } catch (IOException e) {
                System.out.println("sendPlayerPosition 오류> " + e.getMessage());
            }
        }
    }

    // 같은 게임방에 위치한 플레이어들의 위치 업데이트
    public void updateOtherPlayerPosition(String nickname, String characterType, int x, int y) {
        otherPlayers.put(nickname, new int[]{x, y}); // 위치 정보 저장
        otherPlayerCharacters.put(nickname, characterType); // 캐릭터 타입 저장

        repaint();
    }

    // 제한 시간 타이머 스레드
    private void startTimerThread() {
        timerThread = new Thread(() -> {
            try {
                while (isTimeRunning && remainingTime > 0) {
                    Thread.sleep(1000); // 1초 대기
                    remainingTime--; // 시간 감소
                    repaint(); // 패널 갱신
                }
                if (remainingTime == 0) {
                    isTimeRunning = false;
                    stopTimerThread();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        timerThread.start();
    }

    // 제한 시간 타이머 종료
    private void stopTimerThread() {
        System.out.println("시간 초과!"); // 디버깅 출력

        if (timerThread != null && timerThread.isAlive()) {
            isTimeRunning = false; // 루프 중단 플래그 설정
            timerThread.interrupt(); // 스레드 중단
        }
    }

    // 플레이어가 문에 도달했는지 체크
    private boolean isPlayerAtDoor() {
        int playerWidth = 40; // 플레이어의 폭
        int playerHeight = 40; // 플레이어의 높이
        int doorWidth = 60; // 문의 폭
        int doorHeight = 60; // 문의 높이

        // 플레이어와 문 간의 허용 거리 (X, Y 좌표의 오차 허용 범위)
        int xTolerance = 60;
        int yTolerance = 10;

        // 플레이어와 문 간의 충돌 영역 계산
        boolean isXAligned = (playerX + playerWidth > doorX - xTolerance) && (playerX < doorX + doorWidth + xTolerance);
        boolean isYAligned = (playerY + playerHeight > doorY - yTolerance) && (playerY < doorY + doorHeight + yTolerance);

        return isXAligned && isYAligned;
    }

    // 문 열림 상태를 주기적으로 검사
    private void startDoorCheckTimer() {
        Timer doorCheckTimer = new Timer(200, e -> {
            if (!isDoorOpen && isPlayerAtDoor()) {
                isBlocked = true; // 플레이어 움직임 차단
                startDoorAnimation(); // 문 열림 애니메이션 시작
            }
        });
        doorCheckTimer.setRepeats(true); // 계속 반복
        doorCheckTimer.start(); // 타이머 시작
    }

    // 문 열림 효과
    private void startDoorAnimation() {
        Timer doorAnimationTimer = new Timer(3000, new ActionListener() { // 딜레이 설정
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentDoorIndex < doorImages.size() - 1) {
                    currentDoorIndex++; // 다음 문 이미지로 전환
                    repaint(); // 문 상태를 갱신
                    sendCurrentDoorIndex(currentDoorIndex);
                } else {
                    ((Timer) e.getSource()).stop(); // 타이머 종료
                    isDoorOpen = true; // 문이 완전히 열림
                    isBlocked = false; // 플레이어 움직임 허용
                    // 문이 열린 후 추가적인 로직이 필요하면 여기에 작성
                }
            }
        });
        doorAnimationTimer.setRepeats(true); // 계속 반복
        doorAnimationTimer.start(); // 타이머 시작
    }

    // 서버에게 다음 맵으로 전환한다는 메시지 전송
    public void sendNextMap(int level){
        if (out != null) {
            try {
                GameMsg gameMsg = new GameMsg.Builder("NEXT_MAP")
                        .roomName(roomName)
                        .gameMode(mode)
                        .team(team)
                        .nickname(nickName)
                        .character(character)
                        .level(level)
                        .build();

                out.writeObject(gameMsg);
                out.flush();
            } catch (IOException e) {
                System.out.println("sendPlayerPosition 오류> " + e.getMessage());
            }
        }
    }

    private synchronized void sendCurrentDoorIndex(Integer currentDoorIndex) {
        if (out != null) {
            try {
                GameMsg gameMsg = new GameMsg.Builder("DOOR")
                        .roomName(roomName)
                        .gameMode(mode)
                        .team(team)
                        .nickname(nickName)
                        .character(character)
                        .currentDoorIndex(currentDoorIndex)
                        .build();

                out.writeObject(gameMsg);
                out.flush();
            } catch (IOException e) {
                System.out.println("sendPlayerPosition 오류> " + e.getMessage());
            }
        }
    }

    // 현재 맵의 게임이 끝나고 다음 맵으로 전환할 수 있는지 검사
    private void startNextMapThread() {
        Thread nextMapThread = new Thread(() -> {
            while (true) {
                try {
                    // 문이 열리거나 타이머가 종료되었는지 확인
                    if (isDoorOpen || !timerThread.isAlive()) {
                        sendNextMap(level+1); // 다음 레벨로 이동
                        break; // 루프 종료
                    }
                    Thread.sleep(100); // 100ms마다 확인
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // 스레드가 중단되면 루프 종료
                }
            }
        });
        nextMapThread.setDaemon(true); // 데몬 스레드로 설정 (프로그램 종료 시 자동으로 종료)
        nextMapThread.start(); // 스레드 시작
    }

    // 화면 갱신
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 배경 그리기
        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);

        // 현재 레벨의 블록만 그리기
        List<int[]> levelBlocks = blocks.get(level);
        if (levelBlocks != null) {
            for (int[] block : levelBlocks) {
                g.drawImage(blockImage, block[0], block[1], block[2], block[3], this);
            }
        }

        // 문 그리기
        g.drawImage(doorImages.get(currentDoorIndex), doorX, doorY, 60, 60, this);

        // 현재 플레이어 캐릭터 그리기
        Image currentPlayerImage = characterImages.getOrDefault(character, characterImages.get("fire"));
        g.drawImage(currentPlayerImage, playerX, playerY, 40, 40, this);


        // 다른 플레이어 캐릭터 그리기
        for (Map.Entry<String, int[]> entry : otherPlayers.entrySet()) {
            int[] coords = entry.getValue();
            String charType = otherPlayerCharacters.getOrDefault(entry.getKey(), "fire");
            g.drawImage(characterImages.getOrDefault(charType, characterImages.get("fire")),
                    coords[0], coords[1], 40, 40, this);
        }

        // 제한 시간 표시
        g.setColor(Color.RED);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 15));

        // 텍스트 크기 측정
        String timeText = "남은 시간: " + remainingTime + "초";
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(timeText);
        int textHeight = metrics.getHeight();

        // 가운데 정렬 계산
        int x = (getWidth() - textWidth) / 2;
        int y = textHeight + 10; // 위쪽 여백

        // 남은 시간 텍스트 그리기
        g.drawString(timeText, x, y);

    }

    public void setCurrentDoorIndex(int currentDoorIndex) {
        this.currentDoorIndex = currentDoorIndex;
        repaint();
    }
}
