package com.da.chat;

import com.da.util.JsonUtil;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * @author: Kandoka
 * @createTime: 2020/05/21 16:57
 * @description:
 */

public class ChatServer implements Runnable {

    private ChatSocketManager chatSocketManager;

    private JTextArea jta_message, jta_chat;

    private MessageManager messageManager;

    public ChatServer(JTextArea jta_message, JTextArea jta_chat) {
        this.jta_message  = jta_message;
        this.jta_chat = jta_chat;
        messageManager = new MessageManager();
        chatSocketManager = new ChatSocketManager();
    }

    @Override
    public void run() {
        try {
            ServerSocket ss = new ServerSocket(8091) ;
            Thread sender = new Thread(new MessageSender(jta_chat, chatSocketManager, messageManager));
            sender.start();

            while(true)  {
                Socket currentSocket = ss.accept() ;
                String ip = currentSocket.getInetAddress().getHostAddress() ;
                chatSocketManager.addSocket(currentSocket);
                Thread receiver = new Thread(new MessageReceiver(currentSocket, chatSocketManager, messageManager)) ;
                receiver.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(){
        String text = jta_message.getText();
        text = text.replace('\n', ' ');
        Date date = new Date();
        messageManager.addMessage(date,text);
    }

//    public static void main(String[] args) {
//        ChatServer chatServer = new ChatServer();
//        chatServer.run();
//    }
}

class MessageReceiver implements Runnable{
    private ChatSocketManager chatSocketManager;
    private Socket currentSocket;
    private MessageManager messageManager;
    private BufferedReader br;
    private InputStreamReader isr;
    public MessageReceiver(Socket currentSocket, ChatSocketManager chatSocketManager, MessageManager messageManager) {
        this.chatSocketManager = chatSocketManager;
        this.currentSocket = currentSocket;
        this.messageManager = messageManager;
    }

    @Override
    public void run() {
        try  {
            isr = new InputStreamReader(currentSocket.getInputStream());
            br = new BufferedReader(isr) ;
            String jsonStr;

            while((jsonStr = br.readLine()) != null)  {
//                System.out.println("server receives: "+jsonStr);
                messageManager.addMessage(new Date(), jsonStr);
            }
        }catch(IOException e)  {
            chatSocketManager.removeSocket(currentSocket);
        }
    }

}

class MessageSender implements Runnable {
    private JTextArea jta_chat;
    private ChatSocketManager chatSocketManager;
    private MessageManager messageManager;

    public MessageSender(JTextArea jta_chat, ChatSocketManager chatSocketManager, MessageManager messageManager) {
        this.chatSocketManager = chatSocketManager;
        this.messageManager = messageManager;
        this.jta_chat = jta_chat;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if(messageManager.getAllMessage().size() == 0 || chatSocketManager.getAllSocket().size() == 0)
                    Thread.sleep(100);
                else
                    broadcast();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     ** check all followers' liveness
     */
    public void checkSocketsAlive(){
        for(Socket socket: chatSocketManager.getAllSocket()) {
            try {
                socket.sendUrgentData(0xFF);
            } catch (IOException e) {
                //if heart beat check fails, remove the socket
                chatSocketManager.removeSocket(socket);
            }
        }
    }

    /**
     ** broadcast new messages to all followers
     */
    private void broadcast() throws IOException {

        //checkSocketsAlive();

        if(messageManager.getAllMessage().size() == 0 || chatSocketManager.getAllSocket().size() == 0){
            System.out.println("???");
            return;
        }

        for(Socket socket:chatSocketManager.getAllSocket()) {
            sendMessage(socket, messageManager.getAllMessage());
        }

        Set<Map.Entry<Date, String>> entries = messageManager.getAllMessage().entrySet();
        for (Map.Entry<Date, String> entry: entries){
            System.out.println("Date: "+entry.getKey());
            System.out.println("Message: "+entry.getValue());
            jta_chat.append(entry.getKey()+": "+entry.getValue()+"\n");
        }

        messageManager.clearAllMessage();
    }

    /**
     ** send messages to a follower
     */
    private void sendMessage(Socket slaveSocket, Map<Date, String> messages) throws IOException {
        if(slaveSocket.isClosed()){
            return;
        }
        String jsonStr = JsonUtil.map2Json(messages);
        PrintWriter pw;
        pw = new PrintWriter(new OutputStreamWriter(slaveSocket.getOutputStream()));
        pw.println(jsonStr) ;
        pw.flush();
    }
}