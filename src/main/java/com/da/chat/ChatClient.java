package com.da.chat;

import com.da.berkerly.utils.JsonUtil;
import com.da.berkerly.Client;
import com.da.berkerly.data.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatClient extends Client implements Runnable{

    SimpleDateFormat myFmt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//等价于now.toLocaleString()
    Socket socket;

    public ChatClient(Socket socket) {
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
                String currentTime = myFmt.format(new Date(new Date().getTime() + skew));

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
