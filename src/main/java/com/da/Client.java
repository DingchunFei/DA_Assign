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

        //read the message received
        br = new BufferedReader(new InputStreamReader(socket.getInputStream())) ;
        String jsonStr = null ;
        while( (jsonStr = br.readLine()) != null)  {
            Long amountToAdjust = JsonUtil.json2long(jsonStr);
            System.out.println("[current follower time is: ]"+ new Date(currentClock.getCurrentTime()));
            System.out.println("[skew is: ]"+ amountToAdjust);
            if(amountToAdjust == -1l){   //if -1, send current time as a response
                sendCurrentTime(socket);
            }
            else{//else clock needs to be adjusted
                adjustClock(amountToAdjust);
            }
        }
        System.out.println("out of the while");
    }

    public void adjustClock(Long amountToAdjust) {
        if(amountToAdjust > 0l) {//adjust clock forward
            currentClock.updateCurrentTime(amountToAdjust);
        }
        else {//slow the clock
            rate = (int) (rate * 0.9);
        }
    }

    //after receive a request from server, give a response
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

