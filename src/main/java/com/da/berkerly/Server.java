package com.da.berkerly;


import com.da.berkerly.data.Data;
import com.da.berkerly.utils.JsonUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server implements Runnable{

    public volatile static Long skew = 0l;

    public static volatile List<Socket> sockets = new CopyOnWriteArrayList<>() ;    //使用CopyOnWriteArrayList保证线程安全

    public static volatile Map<Socket,Date> socketDateMap = new HashMap<>();

    public static List<Socket> getSockets() {
        return sockets;
    }

    public static Map<Socket, Date> getSocketDateMap() {
        return socketDateMap;
    }


    @Override
    public void run() {
        try {
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

            while(true)  {
                Socket socket = ss.accept() ;
                sockets.add(socket) ;
                String ip = socket.getInetAddress().getHostAddress() ;
                System.out.println("New User coming！ip:"+ip) ;
                Thread thread = new Thread(new ServerNodeRunner(socket)) ;
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void send2Sockets(String jsonStr) throws Exception{
        for(Socket slaveSocket : sockets)  {
            PrintWriter pw = null;
            pw = new PrintWriter(new OutputStreamWriter(slaveSocket.getOutputStream()));
            pw.println(jsonStr) ;
            pw.flush();
        }
    }

    /**
     * 心跳检测
     */
    public void checkSocketsAlive(){
/*        for(Socket socket: sockets) {
            try {
                socket.sendUrgentData(0xFF);
            } catch (IOException e) {
                //心跳检测失败，移除该socket
                sockets.remove(socket);
            }
        }*/
    }

    /**
     * 向各个socket发送获取时间的请求
     */
    public void broadcast() throws Exception{
        Long meanTime = 0l;
        //每次发送时间前把map里的所有内容清空
        socketDateMap.clear();
        //定义好，如果值是-1表示是一个获取时间的请求
        String jsonStr = JsonUtil.data2Json(new Data(-1l));
        //向所有的socket发送一个获取时间的请求
        send2Sockets(jsonStr);

        Long startTime = new Date().getTime();

        System.out.println("当前socket大小 ===> "+ sockets.size());
        //等到获取所有socket传来的时间

        //checkSocketsAlive();

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

        //加上自己的时间求一个平均值
        meanTime = (meanTime + new Date().getTime())/(socketDateMap.size() + 1);
        //重新刷新服务器的skew
        skew = new Date().getTime() - meanTime;

        System.out.println("skew ===> " + skew);

        jsonStr = JsonUtil.data2Json(new Data(meanTime));
        //向所有的socket发送一个获取时间的请求
        send2Sockets(jsonStr);
    }

    public static void putDateIntoMap(Socket socket, Date date){
        socketDateMap.put(socket,date);
    }

    public static void main(String args[])  {
        try {
            Server server = new Server();
            new Thread(server).start();
        } catch(Exception e)  {
            e.printStackTrace();
        }
    }


}


class ServerNodeRunner implements Runnable  {
    private Socket currentSocket ;   //当前socket

    public ServerNodeRunner (Socket currentSocket)  {
        this.currentSocket = currentSocket ;
    }

    public void run()  {
        //SimpleDateFormat myFmt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//等价于now.toLocaleString()

        BufferedReader br;
        try  {
            br = new BufferedReader(new InputStreamReader(currentSocket.getInputStream())) ;
            String jsonStr;
            while((jsonStr = br.readLine()) != null)  {
                Data data = JsonUtil.json2data(jsonStr);
                //说明收到的是客户端发送来的各自时间
                if(data.getCurrentTime()!=null){
                    Date date = new Date(data.getCurrentTime());
                    //将获取到的Date放入Map中
                    Server.putDateIntoMap(currentSocket,date);
                    System.out.println("server收到的"+ currentSocket.getInetAddress().getHostAddress()+"时间===> "+date);
                }else if(data.getMsg()!= null){             //说明收到的是发送的聊天消息
                    System.out.println("服务器收到 ===> "+data.getMsg());
                    data.setMsg(currentSocket.getInetAddress().getHostAddress()+ "  "+data.getMsg());
                    //发送给所有客户端
                    send2AllClients(JsonUtil.data2Json(data));
                }
            }
        }catch(IOException e)  {
            e.printStackTrace();
        }
    }

    private void send2AllClients(String jsonStr) throws IOException {
        PrintWriter out = null;
        for (Socket sc : Server.getSockets()){
            out = new PrintWriter(sc.getOutputStream());
            out.println(jsonStr);
            out.flush();
        }

    }
}

