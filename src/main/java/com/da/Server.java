package com.da;

import com.da.util.JsonUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    //interval to broadcast
    public final static int interval = 5000;
    //allow slight skew
    public final static int lowerBound = 20;
    //ignore too large skew
    public final static int upperBound = 300000;//5 minutes
    //clock of server
    private Clock currentClock;
    //the time when a broadcast starts
    Long broadcastTime;

    private List<Socket> sockets = new CopyOnWriteArrayList<>() ;    //use CopyOnWriteArrayList to ensure thread secure

    private static volatile Map<Socket,Date> socketDateMap = new HashMap<>();

    public Server() throws IOException {
        //create a clock for this machine and set its current time
        currentClock = new Clock(new Date().getTime());
        //a thread to update clock with a specific frequency
        new Thread(()->{
            while(true){
                try {
                    Thread.sleep(1000);
                    currentClock.updateCurrentTime(1000l);
                    //System.out.println("+++++current time++++ "+currentClock.getCurrentTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        ServerSocket ss = new ServerSocket(8090) ;
        System.out.println("Server is listening the port : 8090") ;

        //this thread s used to broadcast request to follower to get their time
        new Thread(()->{
            try{
                while(true){
                    if(sockets.size()!=0) broadcast();
                    Thread.sleep(interval);
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
            System.out.println("[New User coming！ip:]"+ip) ;
            Thread thread = new Thread(new NodeRunner(socket)) ;
            thread.start();
        }
    }

    //send massage to slave servers
    public void send2Sockets(String jsonStr, Socket slaveSocket) throws Exception{
        PrintWriter pw = null;
        pw = new PrintWriter(new OutputStreamWriter(slaveSocket.getOutputStream()));
        pw.println(jsonStr) ;
        pw.flush();
    }

    public void checkSocketsAlive(){
        for(Socket socket: sockets) {
            try {
                socket.sendUrgentData(0xFF);
            } catch (IOException e) {
                //if heart beat check fails, remove the socket
                sockets.remove(socket);
            }
        }
    }

    /**
     * broadcast
     */
    public void broadcast() throws Exception{
        //-1 means get current time of a follower
        Long reqParams = -1l;
        Long meanTime = 0l;
        int numOfIgnore = 0;//number of which servers are ignored

        //System.out.println("[Sever current time: ]" + new Date(meanTime));

        //every time before sending, clear the map
        socketDateMap.clear();
        String jsonStr = JsonUtil.long2Json(reqParams);
        //send request to all follower for their time
        for(Socket slaveSocket : sockets) {
            send2Sockets(jsonStr, slaveSocket);
        }
        //the time when this broadcast starts
        broadcastTime = new Date().getTime();

        System.out.println("[the size of current time] "+ sockets.size());

        checkSocketsAlive();

        while(socketDateMap.size()!=sockets.size()){
            //if any follower does not give a response, abort this broadcast
            if(new Date().getTime() -broadcastTime > 1000){
                return;
            }
        }
        //System.out.println("[server time ++++++]" + meanTime);
        //compute mean time
        meanTime = broadcastTime;//count on leader's time
        Set<Map.Entry<Socket, Date>> entries = socketDateMap.entrySet();
        for (Map.Entry<Socket,Date> entry: entries){
            //if the skew is larger than upper bound, ignore it
            if (upperBound < Math.abs(entry.getValue().getTime() - currentClock.getCurrentTime())) {
                numOfIgnore += 1;
                continue;
            }
            meanTime = meanTime + entry.getValue().getTime();
        }

        //compute mean time
        meanTime = meanTime / (socketDateMap.size() + 1 - numOfIgnore);
        //System.out.println("[meanTime ===> ]" + new Date(meanTime));
        System.out.println("[mean time ++++++]" + meanTime);
        //send amount for each follower that it should adjust by
        for(Map.Entry<Socket,Date> entry: entries) {
            Long amountToAdjust = meanTime - entry.getValue().getTime();
            if(lowerBound > Math.abs(amountToAdjust))//allow slight skew, no need to change
                amountToAdjust = 0l;
            jsonStr = JsonUtil.long2Json(amountToAdjust);
            send2Sockets(jsonStr, entry.getKey());
        }
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
    private Socket currentSocket ;   //current socket

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
                //save time from response to map
                Server.putDateIntoMap(currentSocket,date);
                System.out.println("server receives "+ currentSocket.getInetAddress().getHostAddress()+"time===> "+date);
            }
        }catch(IOException e)  {
            e.printStackTrace();
        }
    }
}

