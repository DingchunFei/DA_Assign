package com.da.berkerly.data;


public class Data {
    public Long currentTime;
    public String msg;

    public Data() {
    }

    public Data(Long currentTime) {
        this.currentTime = currentTime;
    }

    public Data(String msg) {
        this.msg = msg;
    }

    public Long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(Long currentTime) {
        this.currentTime = currentTime;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
