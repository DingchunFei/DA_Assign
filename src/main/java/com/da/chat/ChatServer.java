/*
package com.da.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Vector;

public class ChatServer implements Runnable{
    //将接收到的socket变成一个集合
    protected static  List<Socket> sockets = new Vector<>();

    public volatile ServerSocket ss;

    public ChatServer() {
    }

    public ChatServer(ServerSocket ss) {
        this.ss = ss;
    }

    @Override
    public void run() {

        try {
            boolean flag = true;
            //接受客户端请求
            while (flag){
                try {
                    //阻塞等待客户端的连接
                    Socket accept = ss.accept();
                    synchronized (sockets){
                        sockets.add(accept);
                    }
                    //多个服务器线程进行对客户端的响应
                    Thread thread = new Thread(new ServerThread(accept));
                    thread.start();
                    //捕获异常。
                }catch (Exception e){
                    flag = false;
                    e.printStackTrace();
                }
            }
            //关闭服务器
            ss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
*/
