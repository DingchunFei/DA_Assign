package com.da.chat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: Kandoka
 * @createTime: 2020/05/21 19:58
 * @description:
 */

public class MessageManager {
    private Map<Date, String> messages;

    public MessageManager() {
        this.messages = new HashMap<>();
    }

    public synchronized void addMessage(Date date, String message) {
        messages.put(date, message);
    }

    public synchronized Map<Date, String> getAllMessage() {
        return messages;
    }

    public synchronized void clearAllMessage(){
        messages.clear();
    }
}
