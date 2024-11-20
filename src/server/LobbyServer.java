package server;

import data.Msg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

public class LobbyServer extends JFrame {
    private int port;
    private ServerSocket serverSocket;
    private JTextArea t_display;
    private JButton b_start, b_end, b_exit;
    private Thread acceptThread = null;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();
    private Vector<Lobby> lobbys = new Vector<Lobby>();

    public LobbyServer(int i) {
        super();
        this.port = i;
        this.setTitle("WithChatServer");
        this.setBounds(500, 0, 500, 300);
        this.setLayout(new BorderLayout());

        buildGUI();

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private void buildGUI() {
        JPanel displayPanel, controlPanel, bottomPanel;
        displayPanel = createDisplayPanel();

        controlPanel = createControlPanel();
        bottomPanel = new JPanel(new GridLayout(0, 1));


        bottomPanel.add(controlPanel);

        this.add(displayPanel, BorderLayout.CENTER);
        this.add(bottomPanel, BorderLayout.SOUTH);

        setDisplay_disconn();
    }

    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        t_display = new JTextArea();
        JScrollPane scroll = new JScrollPane(t_display);

        t_display.setEditable(false);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 0));
        b_start = new JButton();
        b_end = new JButton();
        b_exit = new JButton();

        b_start.setText("서버 시작");
        b_end.setText("서버 종료");
        b_exit.setText("종료");

        b_start.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                acceptThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startServer();
                    }
                });
                acceptThread.start();
                setDisplay_conn();
            }

        });

        b_end.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();
                setDisplay_disconn();

            }

        });

        b_exit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }

        });

        panel.add(b_start);
        panel.add(b_end);
        panel.add(b_exit);
        return panel;
    }

    private void startServer() {

        Socket clientSocket = null;
        try {
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            printDisplay("서버가 시작되었습니다: " + port);
            while (acceptThread == Thread.currentThread()) {
                clientSocket = serverSocket.accept();

                String cAddr = clientSocket.getInetAddress().getHostAddress();
                printDisplay("클라이언트가 연결되었습니다: " + cAddr);

                ClientHandler cHandler = new ClientHandler(clientSocket);
                users.add(cHandler);
                cHandler.start();
                //receiveMessages(clientSocket);
            }
        } catch (SocketException e) {
            printDisplay("서버 소켓 종료");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                printDisplay("서버 닫기 오류> " + e.getMessage());
                System.exit(-1);
            }

        }
    }

    private class ClientHandler extends Thread{
        private Socket clientSocket;
        //		private BufferedWriter out;
        private ObjectOutputStream out;
        private String userName;
        private Lobby myLobby;


        public ClientHandler(Socket clientSocket){
            this.clientSocket=clientSocket;
            setDisplay_conn();
        }
        private void send(Msg msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                System.err.println("클라이언트 일반 전송 오류> "+e.getMessage());
            }

        }

        private void broadcasting(Msg msg) {
            for(ClientHandler ch : users) {
                ch.send(msg);
            }
        }

        private void receiveMessages(Socket socket) {

            try {
                ObjectInputStream in=new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                out=new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                String message;
                Msg msg;
                while((msg = (Msg)in.readObject())!=null) {
                    if(msg.mode == Msg.MODE_LOGIN) {
                        userName = msg.userName;

                        printDisplay("새 참가자: "+userName);
                        printDisplay("현재 참가자 수: "+users.size());
                        continue;
                    }else if(msg.mode== Msg.MODE_CREATE_LOBBY){
                        //Todo: 방제목 겹치는지 확인하는 로직 추가
                        if(msg.playMode!=0) {
                            myLobby = new Lobby(msg.lobbyName, msg.lobbyPassword, msg.playMode);
                            lobbys.add(myLobby);
                            myLobby.current_Players++;
                        }

                        printDisplay(myLobby.lobbyName + "방 생성: "+userName);
                        broadcasting(msg);

                    }else if(msg.mode== Msg.MODE_ENTER_LOBBY){
                        for(Lobby l : lobbys){
                            if(l.lobbyName.equals(msg.lobbyName)&&l.lobbyPassword.equals(msg.lobbyPassword)){
                                myLobby=l;
                                myLobby.current_Players++;
                                printDisplay(myLobby.lobbyName+"방 참가: "+userName+", 현재 인원: "+myLobby.current_Players);
                                broadcasting(msg);
                            }
                        }

                    }
                }

                users.removeElement(this);
                myLobby.current_Players--;
                printDisplay(myLobby.lobbyName+"방 참가: "+userName+", 현재 인원: "+myLobby.current_Players);
                if(myLobby.current_Players<=0){
                    lobbys.removeElement(myLobby);
                    printDisplay(myLobby.lobbyName+"방 삭제");
                }
                printDisplay(userName+" 퇴장. 현재 참가자 수: "+users.size());



            }catch(IOException e) {
                users.removeElement(this);
                myLobby.current_Players--;
                printDisplay(myLobby.lobbyName+"방 참가: "+userName+", 현재 인원: "+myLobby.current_Players);
                if(myLobby.current_Players<=0){
                    lobbys.removeElement(myLobby);
                    printDisplay(myLobby.lobbyName+"방 삭제");
                }
                printDisplay(userName+" 연결 끊김. 현재 참가자 수: "+users.size());
            }catch(ClassNotFoundException e) {

            }
            finally {
                try {
                    socket.close();
                }catch(IOException e) {
                    System.err.println("서버 닫기 오류> "+ e.getMessage());
                    System.exit(-1);
                }
            }
        }

        @Override
        public void run() {
            receiveMessages(clientSocket);
        }


    }

    public class Lobby{
        String lobbyName;
        String lobbyPassword;
        int playMode;
        int max_Players;
        int current_Players;
        public Lobby(String lobbyName,String lobbyPassword,int playMode){
            this.lobbyName=lobbyName;
            this.lobbyPassword=lobbyPassword;
            this.playMode=playMode;
            if(playMode==1){
                max_Players =2;
            }
            else if(playMode==2){
                max_Players =4;
            }
            this.current_Players=0;
        }
    }
    private void disconnect() {
        acceptThread = null;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("서버 소켓 닫기 오류> " + e.getMessage());
            System.exit(-1);
        }
    }

    private void printDisplay(String s) {
        t_display.append(s + '\n');
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    private void setDisplay_conn() {
        b_start.setEnabled(false);
        b_end.setEnabled(true);
        b_exit.setEnabled(false);

    }

    private void setDisplay_disconn() {
        b_start.setEnabled(true);
        b_end.setEnabled(false);
        b_exit.setEnabled(true);
    }


    public static void main(String[] args) {
        int port = 54321;
        LobbyServer server = new LobbyServer(port);
    }
}
