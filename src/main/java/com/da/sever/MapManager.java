package com.da.sever;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

/**
 * @author: Kandoka
 * @createTime: 2020/05/21 16:23
 * @description:
 */

class MapManager {
    //a map for each socket and its info, use type volatile to ensure thread secure
    private Map<Socket, Date> socketDateMap;
    //a map for each socket and the thread to handle it
    private Map<Socket, Thread> socketThreadMap;

    MapManager() {
        socketDateMap = new HashMap<>();
        socketThreadMap  = new HashMap<>();
    }

    /**
     * put date into the map contains socket and its follower current time
     */
    public synchronized void putDateIntoMap(Socket socket, Date date) {
        socketDateMap.put(socket,date);
    }

    /**
     * remove date into the map contains socket and its follower current time
     */
    public synchronized void removeDateFromMap(Socket socket) {
        socketDateMap.remove(socket);
    }

    /**
     * remove date into the map contains socket and its follower current time
     */
    public synchronized void clearDateFromMap() {
        socketDateMap.clear();
    }

    /**
     * get all socketDateMap
     */
    public synchronized Map<Socket, Date> getSocketDateMap() {
        return socketDateMap;
    }

    /**
     * put socket and its handler thread into the map
     */
    public synchronized void putThreadIntoMap(Socket socket, Thread thread) {
        socketThreadMap.put(socket,thread);
        System.out.println("=======Put:"+socket+thread);
        for(Object o : socketThreadMap.keySet()){
            System.out.println("Now: "+o+": "+socketThreadMap.get(o));
        }
    }

    /**
     * remove socket and its handler thread into the map
     * stop thread in a proper way
     */
    public synchronized void removeThreadFromMap(Socket socket) {
        String ip = socket.getInetAddress().getHostAddress();
        Thread thread = socketThreadMap.get(socket);
        System.out.println("=======remove: "+thread);
        thread.interrupt();
        try {
            socket.shutdownOutput();
            socket.shutdownInput();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        socketThreadMap.remove(socket);
        for(Object o : socketThreadMap.keySet()){
            System.out.println("Now: "+o+": "+socketThreadMap.get(o));
        }
        System.out.println("A follower close its connection: "+ip);
    }

    public synchronized List<Socket> getAllSocket() {
        List<Socket> list = new ArrayList<>();
        for(Map.Entry<Socket, Thread> entry : socketThreadMap.entrySet()){
            Socket mapKey = entry.getKey();
            list.add(mapKey);
        }
        return list;
    }
}
