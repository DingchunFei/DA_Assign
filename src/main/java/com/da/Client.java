package com.da;

import com.da.util.JsonUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Client {

    private Clock currentClock;
    private int rate = 900;

    public Client() throws IOException {
        //create a clock for this machine and set its current time
        currentClock = new Clock(new Date().getTime());
        rate += new Random().nextInt(100);

        //a thread to update clock with a specific frequency
        new Thread(()->{
            while(true){
                try {
                    Thread.sleep(1000);
                    currentClock.updateCurrentTime(1l * rate);
                    System.out.println("+++++current time++++ "+currentClock.getCurrentTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Socket socket = new Socket("localhost",8090) ;
        BufferedReader br = null ;

        //读取服务器的数据
        br = new BufferedReader(new InputStreamReader(socket.getInputStream())) ;
        String jsonStr = null ;
        while( (jsonStr = br.readLine()) != null)  {
            Long amountToAdjust = JsonUtil.json2long(jsonStr);
            System.out.println("[current follower time is: ]"+ new Date(currentClock.getCurrentTime()));
            System.out.println("[skew is: ]"+ amountToAdjust);
            if(amountToAdjust == -1l){   //确保是服务器发来的获取时间的请求，即值为-1
                sendCurrentTime(socket);
            }
            else{//clock needs to be adjusted
                adjustClock(amountToAdjust);
            }
        }
        System.out.println("out of the while");
    }

    public void adjustClock(Long amountToAdjust) {
        if(amountToAdjust > 0l) {//adjust clock forward
            currentClock.updateCurrentTime(amountToAdjust);
        }
        else {//slow the clock TODO

        }
    }

    //收到服务器请求后，向服务器发送时间
    public void sendCurrentTime(Socket socket){
        Long currentTime = new Date().getTime();
        String jsonStr = JsonUtil.long2Json(currentTime);
        PrintWriter out;
        try  {
            //send the data to server
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream())) ;
            out.println(jsonStr) ;
            out.flush();
        }catch(Exception e1)  {
            e1.printStackTrace();
        }
    }

    public static void main(String args[])  {
        try {
            new Client() ;
        } catch(Exception e)  {
            e.printStackTrace();
        }
    }
}

