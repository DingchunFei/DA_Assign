package com.da.sever;

import com.da.chat.ChatUI;
import com.da.common.Clock;
import com.da.util.JsonUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    public final static int interval = 5000;//interval to broadcast

    public final static int lowerBound = 200;//allow slight skew

    public final static int upperBound = 5*1000;//ignore too large skew//5 seconds

    private Clock currentClock;//clock of this server

    private static volatile Long broadcastTime;//the time when a broadcast starts

    private int rate = 1000;//clock rate

    private MapManager mapManager;//a manager manages info about socket

    public Server() throws IOException {
        //create a clock for this machine and set its current time
        currentClock = new Clock(new Date().getTime());
        mapManager = new MapManager();

        ServerSocket ss = new ServerSocket(8090) ;
        System.out.println("Server is listening the port : 8090") ;

        //a thread to update clock with a specific frequency
        new Thread(()->{
            while(true){
                try {
                    Thread.sleep(1000);
                    currentClock.updateCurrentTime(1l * rate);
//                    System.out.println("[current the master's time: ]"+new Date(currentClock.getCurrentTime())+" [Rate: ]"+rate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //this thread is used to broadcast request to follower to get their time
        new Thread(()->{
            try{
                while(true){
                    if(mapManager.getAllSocket().size()!=0) broadcast();
                    Thread.sleep(interval);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();

        //create a new thread to handle a coming follower
        while(true)  {
            Socket currentSocket = ss.accept() ;
            String ip = currentSocket.getInetAddress().getHostAddress() ;
            System.out.println("[New User coming！ip:]"+ip) ;
            Thread thread = new Thread(new TimeReceiver(currentSocket, currentClock, mapManager)) ;
            mapManager.putThreadIntoMap(currentSocket, thread);
            thread.start();
        }
    }

    /**
     ** send massage to slave servers
     */
    public void send2Sockets(String jsonStr, Socket slaveSocket) throws Exception{
        if(slaveSocket.isClosed()){
            return;
        }
        PrintWriter pw = null;
        pw = new PrintWriter(new OutputStreamWriter(slaveSocket.getOutputStream()));
        pw.println(jsonStr) ;
        pw.flush();
    }

    /**
     ** check all follower's liveness
     */
    public void checkSocketsAlive(){
        for(Socket socket: mapManager.getAllSocket()) {
            try {
                socket.sendUrgentData(0xFF);
            } catch (IOException e) {
                //if heart beat check fails, remove the socket
                mapManager.removeThreadFromMap(socket);
            }
        }
    }

    /**
    ** adjust time on this server
     */
    private void adjustClock(Long amountToAdjust) {
        if(amountToAdjust > 0l) {//adjust clock forward
            System.out.println("[Clock is slower. Should be adjust by: ]" + amountToAdjust);
            currentClock.updateCurrentTime(amountToAdjust);
        }
        else {//slow the clock
            System.out.println("[Clock is quicker. The skew is: ]" + amountToAdjust);
//            rate = (int) (rate * 0.99);
//            System.out.println("[Now clock rate has been adjust as: ]" + rate);
            System.out.println("However as a master, rate had better to stay unchanged]");
        }
    }

    /**
     * broadcast to all follower
     */
    public void broadcast() throws Exception{
        //-1 means get current time of a follower
        Long reqParams = -1l;
        Long meanTime = 0l;
        int numOfIgnore = 0;//number of which servers are ignored
        //every time before sending, clear the map
        mapManager.clearDateFromMap();

        String jsonStr = JsonUtil.long2Json(reqParams);
        //send request to all follower for their time
        for(Socket slaveSocket : mapManager.getAllSocket()) {
            send2Sockets(jsonStr, slaveSocket);
        }
        //the time when this broadcast starts
        setBroadcastTime(currentClock.getCurrentTime());

        System.out.println("[The size of current follower] "+ mapManager.getAllSocket().size());

        checkSocketsAlive();

        //waiting until all of followers give their response
        while(mapManager.getSocketDateMap().size()!=mapManager.getAllSocket().size()){
            //if any follower does not give a response, abort this broadcast
            if(currentClock.getCurrentTime() - getBroadcastTime() > 4*1000){
                return;
            }
        }
        //compute mean time
        meanTime = getBroadcastTime();//count on leader's time
        Set<Map.Entry<Socket, Date>> entries = mapManager.getSocketDateMap().entrySet();
        for (Map.Entry<Socket, Date> entry: entries){
            //if the skew is larger than upper bound, ignore it
            if (upperBound < Math.abs(entry.getValue().getTime() - currentClock.getCurrentTime())) {
                numOfIgnore += 1;
                continue;
            }
            meanTime = meanTime + entry.getValue().getTime();
        }

        //compute mean time
        meanTime = meanTime / (mapManager.getSocketDateMap().size() + 1 - numOfIgnore);
        System.out.println("[Mean time compute result :]" + new Date(meanTime));

        //adjust master's time, if it is beyond lowerBound
        if (lowerBound < Math.abs(meanTime - broadcastTime))
            adjustClock(meanTime - broadcastTime);
        //send amount for each follower to adjust their time
        for(Map.Entry<Socket,Date> entry: entries) {
            Long amountToAdjust = meanTime - entry.getValue().getTime();
            if(lowerBound > Math.abs(amountToAdjust))//allow slight skew, no need to change
                amountToAdjust = 0l;
            jsonStr = JsonUtil.long2Json(amountToAdjust);
            send2Sockets(jsonStr, entry.getKey());
        }
    }

    /**
     * set time of when broadcast
     */
    public static void setBroadcastTime(Long broadcastTime) {
        Server.broadcastTime = broadcastTime;
    }

    /**
     * get time of when broadcast
     */
    public static Long getBroadcastTime() {
        return Server.broadcastTime;
    }

    public static void main(String args[])  {
        try {
            Thread chat = new Thread(new ChatUI("master"));
            chat.start();
            new Server();
        } catch(Exception e)  {
            e.printStackTrace();
        }
    }
}





