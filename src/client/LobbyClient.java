package client;

import FrameBuilder.ConnectFrameBuilder;
import FrameBuilder.MainFrameBuilder;
import data.Msg;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class LobbyClient {
    private String userName, serverAddr;
    private int portNum,playMode;
    private ObjectOutputStream out;
    private Thread receiveThread = null;
    private Socket socket;
    ConnectFrameBuilder connectFrameBuilder;
    MainFrameBuilder mainFrameBuilder;



    public LobbyClient(String s, int i){
        connectFrameBuilder = new ConnectFrameBuilder();
        connectFrameBuilder.buildGUI(s,i);

        this.serverAddr = s;
        this.portNum = i;
        connectFrameBuilder.b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    //userName
                    String name = connectFrameBuilder.t_userName.getText();
                    userName = name;

                    //서버 연결
                    connectToServer();
                    sendUserName();

                    //mainFrame 열기, ActionListener등록
                    mainFrameBuilder = new MainFrameBuilder();
                    mainFrameBuilder.buildGUI(name);
                    connectFrameBuilder.setVisible(false);
                    setMainFrameAction();
                }catch(UnknownHostException e1) {
                    return;
                }catch(IOException e1) {
                    return;
                }
            }
        });
        connectFrameBuilder.b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });


    }
    private void connectToServer() throws UnknownHostException, IOException {
        Socket socket = new Socket();
        SocketAddress sa = new InetSocketAddress(serverAddr,portNum);
        socket.connect(sa,3000);

        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        receiveThread = new Thread(new Runnable() {

            private ObjectInputStream in;

            private void receiveMessage() {


                try {
                    Msg inMsg = (Msg)in.readObject();
                    if(inMsg==null) {
                        disconnect();
                        return;
                    }

                    switch(inMsg.mode) {
                        //Todo: ChatMode 추가
//                        case ChatMsg.MODE_TX_STRING:
//                            printDisplay(inMsg.userName + ": " + inMsg.message);
//                            break;
//                        case ChatMsg.MODE_TX_IMAGE:
//                            printDisplay(inMsg.userName + ": "+inMsg.message);
//                            printDisplay(inMsg.image);
//                            break;
//                        case ChatMsg.MODE_TX_FILE:
//                            printDisplay(inMsg.userName + ": "+inMsg.message);
//                            receiveFile(inMsg.file, inMsg.message);
//                            break;
                        case Msg.MODE_CREATE_LOBBY:
                            mainFrameBuilder.printLobbyList("방 이름: "+inMsg.lobbyName+",생성자: "+inMsg.userName+",게임모드: "+inMsg.playMode);
                            break;
                        case Msg.MODE_SEARCH_LOBBY:
                            break;

                    }
                } catch (ClassNotFoundException e) {}
                catch (IOException e) {}



            }
            private void receiveFile(File inputFile, String filename) {
                File outputFile = new File(filename);
                try {
                    BufferedInputStream fi=new BufferedInputStream(new FileInputStream(inputFile));
                    BufferedOutputStream fo=new BufferedOutputStream(new FileOutputStream(outputFile));
                    byte[] buffer = new byte[1024];
                    int nRead;
                    while((nRead=fi.read(buffer))!=-1) {
                        fo.write(buffer,0,nRead);
                    }
                    fi.close();
                    fo.close();
                } catch (FileNotFoundException e) {

                } catch(IOException e) {

                }


            }

            @Override
            public void run() {
                try {
                    in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                } catch (IOException e) {}
                while(receiveThread == Thread.currentThread()) {
                    receiveMessage();
                }

            }

        });
        receiveThread.start();
    }

    private void disconnect() {
        send(new Msg(userName, Msg.MODE_LOGOUT));

        try {
            receiveThread = null;
            socket.close();
        } catch (IOException e) {
            System.err.println("클라이언트 닫기 오류> "+ e.getMessage());
            System.exit(-1);
        }
    }
    private void setMainFrameAction(){
        mainFrameBuilder.b_playMode1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playMode=1;
            }
        });

        mainFrameBuilder.b_playMode2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playMode=2;
            }
        });

        mainFrameBuilder.b_createLobby.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String lobbyName = mainFrameBuilder.t_lobbyName.getText();
                String lobbyPassword = mainFrameBuilder.t_lobbyPassword.getText();
                requestLobby(new Msg(userName, Msg.MODE_CREATE_LOBBY,lobbyName,lobbyPassword,playMode));
            }
        });

        mainFrameBuilder.b_enterLobby.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String lobbyName = mainFrameBuilder.t_lobbyName.getText();
                String lobbyPassword = mainFrameBuilder.t_lobbyPassword.getText();
                requestLobby(new Msg(userName, Msg.MODE_ENTER_LOBBY,lobbyName,lobbyPassword));
            }
        });
    }
    private void send(Msg msg){
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void sendUserName(){
        send(new Msg(userName, Msg.MODE_LOGIN));
    }

    private void requestLobby(Msg msg){
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("클라이언트 일반 전송 오류> "+e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = 54321;
        String hostAddr = "localhost";
        LobbyClient client = new LobbyClient(hostAddr,port);
    }
}