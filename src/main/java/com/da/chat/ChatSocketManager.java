package com.da.chat;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Kandoka
 * @createTime: 2020/05/21 17:09
 * @description:
 */

public class ChatSocketManager {
    private List<Socket> sockets;

    public ChatSocketManager() {
        sockets = new ArrayList<>();
    }

    public synchronized void removeSocket(Socket socket) {
        sockets.remove(socket);
    }

    public synchronized void addSocket(Socket socket) {
        sockets.add(socket);
    }

    public List<Socket> getAllSocket() {
        return sockets;
    }
}
