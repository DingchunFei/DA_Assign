package com.da.sever;

import com.da.common.Clock;
import com.da.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author: Kandoka
 * @createTime: 2020/05/21 16:24
 * @description:
 */

class TimeReceiver implements Runnable {
    private Socket currentSocket;   //current socket
    private Clock currentClock; //server's clock
    private MapManager mapManager;//server's MapManager
    private Long tRound = 0l;
    //    private int n = 0;
    private BufferedReader br;
    private InputStreamReader isr;

    public TimeReceiver (Socket currentSocket, Clock currentClock, MapManager mapManager)  {
        this.currentSocket = currentSocket;
        this.currentClock = currentClock;
        this.mapManager = mapManager;
    }

    public void run()  {
        try  {
            isr = new InputStreamReader(currentSocket.getInputStream());
            br = new BufferedReader(isr) ;
            String jsonStr;

            while((jsonStr = br.readLine()) != null)  {
                tRound = currentClock.getCurrentTime() - Server.getBroadcastTime();
//                System.out.println("Tround is: "+tRound);
                Long followerTime = JsonUtil.json2long(jsonStr);
                followerTime = followerTime - tRound/2;
                Date date = new Date(followerTime);
                //save time from response to map
                mapManager.putDateIntoMap(currentSocket, date);
                System.out.println("server receives: "+ currentSocket.getInetAddress().getHostAddress()+" time===> "+new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(date));
                System.out.println("[Tround is: ]"+tRound);
            }
        }catch(IOException e)  {
            mapManager.removeThreadFromMap(currentSocket);
        }
    }
}
