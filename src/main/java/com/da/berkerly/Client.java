package com.da.berkerly;

import com.da.berkerly.data.Data;
import com.da.berkerly.utils.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Client implements Runnable{

    public volatile static Long skew = 0l;

    public volatile  Socket socket;

    public void synchronizeTime(Socket socket,Long currentTime){
        SimpleDateFormat myFmt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//等价于now.toLocaleString()

        //System.out.println("currentTime 是 ===>" + currentTime);
        if(currentTime==-1l){   //确保是服务器发来的获取时间的请求，即值为-1
            sendCurrentTime(socket);
        }
        else{
            //矫正当前漂移
            skew = new Date().getTime() - currentTime;
            System.out.println("skew is: "+skew);
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            socket = new Socket("localhost",8090) ;
            BufferedReader br = null ;

            //读取服务器的数据
            br = new BufferedReader(new InputStreamReader(socket.getInputStream())) ;
            String jsonStr = null ;
            while( (jsonStr = br.readLine()) != null)  {
                Data data = JsonUtil.json2data(jsonStr);
                if(data.getCurrentTime()!=null){
                    synchronizeTime(socket,data.getCurrentTime());
                }else if(data.getMsg()!=null){
                    System.out.println(data.getMsg());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    //收到服务器请求后，向服务器发送时间
    public void sendCurrentTime(Socket socket){
        Long currentTime = new Date().getTime() - skew;
        Data data = new Data();
        data.setCurrentTime(currentTime);
        String jsonStr = JsonUtil.data2Json(data);
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
            Client client = new Client();
            new Thread(client).start();
            //等待上面的链接建立完socket连接后再执行
            while(client.getSocket()==null);

            new Thread(new ClientNodeRunner(client.getSocket())).start();

        } catch(Exception e)  {
            e.printStackTrace();
        }
    }
}

class ClientNodeRunner implements Runnable{

    SimpleDateFormat myFmt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//等价于now.toLocaleString()
    Socket socket;

    public ClientNodeRunner(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            //获取系统标准输入流
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            //从cmd读入输入数据
            String line = reader.readLine();
            while (!"end".equalsIgnoreCase(line)) {
                //将数据放入data的数据结构
                String currentTime = myFmt.format(new Date(new Date().getTime() + Client.skew));

                String jsonStr = JsonUtil.data2Json(new Data(currentTime+"    "+line));
                //System.out.println("jsonStr ===>"+jsonStr);
                //将从键盘获取的信息给到服务器
                out.println(jsonStr);
                out.flush();
                //显示输入的信息
                line = reader.readLine();
            }
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}