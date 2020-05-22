package com.da.common;

/**
 * @author: Kandoka
 * @createTime: 2020/05/05 19:22
 * @description:
 */

public class Clock {
    private Long currentTime;

    public Clock(Long currentTime) {
        this.currentTime = currentTime;
    }

    /**
     * see current time
     */
    public synchronized Long getCurrentTime() {
        return currentTime;
    }

    /**
     * update current time by an amount
     */
    public synchronized void updateCurrentTime(Long amountToAdjust) {
        this.currentTime += amountToAdjust;
    }
}
