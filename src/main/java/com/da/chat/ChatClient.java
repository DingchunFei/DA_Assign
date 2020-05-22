package com.da.chat;

import com.da.util.JsonUtil;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * @author: Kandoka
 * @createTime: 2020/05/21 18:08
 * @description:
 */

public class ChatClient implements Runnable {

    private Socket socket = null;
    private JTextArea jta_message, jta_chat;

    public ChatClient(JTextArea jta_message, JTextArea jta_chat) {
        this.jta_message  = jta_message;
        this.jta_chat = jta_chat;
    }

    @Override
    public void run() {
        try {
            socket = new Socket("localhost",8091);
            //read the message received
            BufferedReader br = null ;
            br = new BufferedReader(new InputStreamReader(socket.getInputStream())) ;
            String jsonStr = null ;
            while( (jsonStr = br.readLine()) != null)  {
                Map<Date, String> map = JsonUtil.json2Map(jsonStr);
                Set<Map.Entry<Date, String>> entries = map.entrySet();
                for (Map.Entry<Date, String> entry: entries){
//                    System.out.println("Date: "+entry.getKey());
//                    System.out.println("Message: "+entry.getValue());
                    jta_chat.append(entry.getKey()+": "+entry.getValue()+"\n");
                }
            }
            System.out.println("out of the while");
        } catch (IOException e) {
            try {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendMessage() {
        PrintWriter out;
        try  {
            String text = jta_message.getText();
            text = text.replace('\n', ' ');
            Date date = new Date();
            //send the data to server
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream())) ;
            out.println(text);
            out.flush();
        }catch(Exception e1) {
            e1.printStackTrace();
        }
    }
}
