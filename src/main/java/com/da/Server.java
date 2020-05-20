package com.da;

import com.da.util.JsonUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    public final static int interval = 5000;//interval to broadcast

    public final static int lowerBound = 200;//allow slight skew

    public final static int upperBound = 5*1000;//ignore too large skew//5 seconds

    private Clock currentClock;//clock of this server

    private static volatile Long broadcastTime;//the time when a broadcast starts

    private int rate = 1000;//clock rate
    //a list of all sockets, use CopyOnWriteArrayList to ensure thread secure
    private List<Socket> sockets = new CopyOnWriteArrayList<>() ;
    //a map for each socket and its info, use type volatile to ensure thread secure
    private static volatile Map<Socket, Date> socketDateMap = new HashMap<>();

    public Server() throws IOException {
        //create a clock for this machine and set its current time
        currentClock = new Clock(new Date().getTime());

        ServerSocket ss = new ServerSocket(8090) ;
        System.out.println("Server is listening the port : 8090") ;

        //a thread to update clock with a specific frequency
        new Thread(()->{
            while(true){
                try {
                    Thread.sleep(1000);
                    currentClock.updateCurrentTime(1l * rate);
                    System.out.println("[current the master's time: ]"+new Date(currentClock.getCurrentTime())+" [Rate: ]"+rate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //this thread is used to broadcast request to follower to get their time
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
            Socket currentSocket = ss.accept() ;
            sockets.add(currentSocket) ;
            String ip = currentSocket.getInetAddress().getHostAddress() ;
            System.out.println("[New User coming！ip:]"+ip) ;
            Thread thread = new Thread(new TimeReceiver(currentSocket, currentClock)) ;
            thread.start();
        }
    }

    /**
     ** send massage to slave servers
     */
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
    ** adjust time on this server
     */
    private void adjustClock(Long amountToAdjust) {
        if(amountToAdjust > 0l) {//adjust clock forward
            System.out.println("[Clock is slower. Should be adjust by: ]" + amountToAdjust);
            currentClock.updateCurrentTime(amountToAdjust);
        }
        else {//slow the clock
            System.out.println("[Clock is quicker. The skew is: ]" + amountToAdjust);
            rate = (int) (rate * 0.99);
            System.out.println("[Now clock rate has been adjust as: ]" + rate);
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
        socketDateMap.clear();

        String jsonStr = JsonUtil.long2Json(reqParams);
        //send request to all follower for their time
        for(Socket slaveSocket : sockets) {
            send2Sockets(jsonStr, slaveSocket);
        }
        //the time when this broadcast starts
        setBroadcastTime(currentClock.getCurrentTime());

        System.out.println("[The size of current follower] "+ sockets.size());

        checkSocketsAlive();

        //waiting until all of followers give their response
        while(socketDateMap.size()!=sockets.size()){
            //if any follower does not give a response, abort this broadcast
            if(currentClock.getCurrentTime() - getBroadcastTime() > 4*1000){
                return;
            }
        }
        //System.out.println("[server time ++++++]" + meanTime);
        //compute mean time
        meanTime = getBroadcastTime();//count on leader's time
        Set<Map.Entry<Socket, Date>> entries = socketDateMap.entrySet();
        for (Map.Entry<Socket, Date> entry: entries){
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
     * put data into the map contains socket and its follower current time
     */
    public static void putDateIntoMap(Socket socket, Date date) {
        socketDateMap.put(socket,date);
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
            new Server();
        } catch(Exception e)  {
            e.printStackTrace();
        }
    }
}


class TimeReceiver implements Runnable  {
    private Socket currentSocket;   //current socket
    private Clock currentClock; //current clock
    private Long tRound = 0l;

    public TimeReceiver (Socket currentSocket, Clock currentClock)  {
        this.currentSocket = currentSocket;
        this.currentClock = currentClock;
    }

    public void run()  {
        BufferedReader br;
        try  {
            br = new BufferedReader(new InputStreamReader(currentSocket.getInputStream())) ;
            String jsonStr;
            while((jsonStr = br.readLine()) != null)  {
                tRound = currentClock.getCurrentTime() - Server.getBroadcastTime();
                System.out.println("Tround is: "+tRound);
                Long followerTime = JsonUtil.json2long(jsonStr);
                followerTime = followerTime - tRound/2;
                Date date = new Date(followerTime);
                //save time from response to map
                Server.putDateIntoMap(currentSocket, date);
                System.out.println("server receives: "+ currentSocket.getInetAddress().getHostAddress()+" time===> "+date);
            }
        }catch(IOException e)  {
            e.printStackTrace();
        }
    }
}


