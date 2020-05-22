package com.da.chat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * @author: Kandoka
 * @createTime: 2020/05/21 16:50
 * @description:
 */

public class ChatUI extends JFrame implements Runnable{
    private JPanel jp_chat=new JPanel();
    public JTextArea jta_chat=new JTextArea();
    private JScrollPane jsp_chat=new JScrollPane(jta_chat);
    private JPanel jp_send=new JPanel();
//    public static  JTextField jtf_desip=new JTextField("Input Ip");
    public  JTextArea jta_message=new JTextArea();
    private JScrollPane jsp_message=new JScrollPane(jta_message);
    private JButton bt_send=new JButton("Send");
    private ChatServer chatServer;
    private ChatClient chatClient;
    private String serverName;

    public ChatUI(String serverName) {
        this.serverName = serverName;

        jp_chat.add(jsp_chat);
        jta_chat.setLineWrap(true);
        jta_chat.setEditable(false);
        jsp_chat.setPreferredSize(new Dimension(550, 400));
        jp_send.add(jsp_message);
        jta_message.setLineWrap(true);
        jp_send.add(bt_send);
        jsp_message.setPreferredSize(new Dimension(250, 50));
        bt_send.setPreferredSize(new Dimension(100, 50));
        jta_message.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                jta_message.setText("");
            }
        });
        bt_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if(serverName.equals("master"))
                    chatServer.sendMessage();
                else if(serverName.equals("follower"))
                    chatClient.sendMessage();
                jta_message.setText("");
            }
        });
        add(jp_chat, BorderLayout.CENTER);
        add(jp_send, BorderLayout.SOUTH);
        setTitle("Group chatting room");
        setBounds(300, 100, 600, 500);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    @Override
    public void run() {
        if(serverName.equals("master")){
            System.out.println("master is running");
            chatServer = new ChatServer(jta_message, jta_chat);
            Thread thread = new Thread(chatServer);
            thread.start();
        }
        else if(serverName.equals("follower")){
            System.out.println("follower is running");
            chatClient = new ChatClient(jta_message, jta_chat);
            Thread thread = new Thread(chatClient);
            thread.start();
        }
    }

    //    public static void main(String[] args) {
//        new ChatUI("follower");
//    }
}
