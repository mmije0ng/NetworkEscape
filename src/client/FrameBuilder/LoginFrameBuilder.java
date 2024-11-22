package client.FrameBuilder;

import client.service.GameClientService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrameBuilder extends JPanel {
    private JTextField t_serverAddr,t_portNum,t_userName;
    private JButton b_connect, b_exit;
    private JPanel inputPanel,buttonPanel;

    private GameClientService service;

    public LoginFrameBuilder(GameClientService service,String serverAddress,int serverPort){
        this.service = service;
        buildGUI(serverAddress,serverPort);
    }

    public void buildGUI(String serverAddr, int portNum){

//        setTitle("미로 대탈출 - 접속 화면");
        setSize(400, 400);
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 서버 주소
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("서버 주소"), gbc);

        gbc.gridx = 1;
        t_serverAddr = new JTextField(serverAddr,20);
        add(t_serverAddr, gbc);

        // 포트 번호
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("포트 번호"), gbc);

        gbc.gridx = 1;
        t_portNum = new JTextField(String.valueOf(portNum),20);
        add(t_portNum, gbc);

        // UserName
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("UserName"), gbc);

        gbc.gridx = 1;
        t_userName = new JTextField(20);
        add(t_userName, gbc);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        b_connect = new JButton("접속하기");
        b_exit = new JButton("종료하기");

        b_connect.addActionListener(e -> service.connectToServer(
                t_serverAddr.getText(),
                Integer.parseInt(t_portNum.getText()),
                t_userName.getText()
        ));

        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(-1);
            }
        });


        buttonPanel.add(b_connect);
        buttonPanel.add(b_exit);


        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        setVisible(true);
    }


}
