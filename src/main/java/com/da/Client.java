package com.da;

import com.da.util.JsonUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Client {

    Long skew = 0l;

    public Long getSkew() {
        return skew;
    }

    public void setSkew(Long skew) {
        this.skew = skew;
    }

    public Client() throws IOException {
        Socket socket = new Socket("localhost",8090) ;
        BufferedReader br = null ;

        //读取服务器的数据
        br = new BufferedReader(new InputStreamReader(socket.getInputStream())) ;
        String jsonStr = null ;
        while( (jsonStr = br.readLine()) != null)  {
            Long currentTime = JsonUtil.json2long(jsonStr);
            System.out.println("currentTime 是 ===>" + currentTime);
            if(currentTime==-1l){   //确保是服务器发来的获取时间的请求，即值为-1
                sendCurrentTime(socket);
            }
            else{
                //矫正当前漂移
                setSkew(currentTime - new Date().getTime());
                System.out.println("current machine time is: "+ new Date());
                System.out.println("current server time is: "+ new Date(new Date().getTime()+skew));
                System.out.println("skew is: "+skew);
            }
        }
    }

    //收到服务器请求后，向服务器发送时间
    public void sendCurrentTime(Socket socket){
        Long currentTime = skew + new Date().getTime();
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

