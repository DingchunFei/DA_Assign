package com.da;

import com.da.util.JsonUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    private List<Socket> sockets = new CopyOnWriteArrayList<>() ;    //使用CopyOnWriteArrayList保证线程安全

    private static volatile Map<Socket,Date> socketDateMap = new HashMap<>();

    public Server() throws IOException {
        ServerSocket ss = new ServerSocket(8090) ;
        System.out.println("Server is listening the port : 8090") ;

        //这个线程专门用来发送服务器获取时间的请求
        new Thread(()->{
            try{
                while(true){
                    if(sockets.size()!=0) broadcast();
                    Thread.sleep(2000);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
        //create a new thread to handle a coming request
        while(true)  {
            Socket socket = ss.accept() ;
            sockets.add(socket) ;
            String ip = socket.getInetAddress().getHostAddress() ;
            System.out.println("New User coming！ip:"+ip) ;
            Thread thread = new Thread(new NodeRunner(socket)) ;
            thread.start();
        }
    }

    //send massage to slave servers
    public void send2Sockets(String jsonStr) throws Exception{
        for(Socket slaveSocket : sockets)  {
            PrintWriter pw = null;
            pw = new PrintWriter(new OutputStreamWriter(slaveSocket.getOutputStream()));
            pw.println(jsonStr) ;
            pw.flush();
        }
    }

    public void checkSocketsAlive(){
        for(Socket socket: sockets) {
            try {
                socket.sendUrgentData(0xFF);
            } catch (IOException e) {
                //心跳检测失败，移除该socket
                sockets.remove(socket);
            }
        }
    }

    /**
     * 向各个socket发送获取时间的请求
     */
    public void broadcast() throws Exception{
        //定义好，如果值是-1表示是一个获取时间的请求
        Long reqParams = -1l;
        Long meanTime = 0l;
        //每次发送时间前把map里的所有内容清空
        socketDateMap.clear();
        String jsonStr = JsonUtil.long2Json(reqParams);
        //向所有的socket发送一个获取时间的请求
        send2Sockets(jsonStr);
        //the time when this broadcast starts
        Long startTime = new Date().getTime();

        System.out.println("当前socket大小 ===> "+ sockets.size());
        //等到获取所有socket传来的时间

        checkSocketsAlive();

        while(socketDateMap.size()!=sockets.size()){
            //如果超过一秒后仍然未收全所有socket的时间，这次广播作废，等待下一轮广播
            if(new Date().getTime() -startTime > 1000){
                return;
            }
        }

        Set<Map.Entry<Socket, Date>> entries = socketDateMap.entrySet();
        for (Map.Entry<Socket,Date> entry: entries){
            meanTime = meanTime + entry.getValue().getTime();
        }

        //求一个平均值
        meanTime = meanTime / socketDateMap.size();
        System.out.println("meanTime ===> " + meanTime);

        jsonStr = JsonUtil.long2Json(meanTime);
        //向所有的socket发送一个获取时间的请求
        send2Sockets(jsonStr);
    }

    public static void putDateIntoMap(Socket socket, Date date){
        socketDateMap.put(socket,date);
    }

    public static void main(String args[])  {
        try {
            Server server = new Server();
        } catch(Exception e)  {
            e.printStackTrace();
        }
    }
}


class NodeRunner implements Runnable  {
    private Socket currentSocket ;   //当前socket

    public NodeRunner (Socket currentSocket)  {
        this.currentSocket = currentSocket ;
    }

    public void run()  {
        BufferedReader br;
        try  {
            br = new BufferedReader(new InputStreamReader(currentSocket.getInputStream())) ;
            String jsonStr;
            while((jsonStr = br.readLine()) != null)  {
                Long currentTime = JsonUtil.json2long(jsonStr);
                Date date = new Date(currentTime);
                //将获取到的Date放入Map中
                Server.putDateIntoMap(currentSocket,date);
                System.out.println("server收到的"+ currentSocket.getInetAddress().getHostAddress()+"时间===> "+date);
            }
        }catch(IOException e)  {
            e.printStackTrace();
        }
    }
}

