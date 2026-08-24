package com.honzel.core.util.time;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Objects;

/**
 * time range
 * @author honzel
 * date 2021/1/4
 */
public class TimeRange implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 开始时间点
     */
    private LocalTime startTime;

    /**
     * 结束时间点
     */
    private LocalTime endTime;

    public TimeRange() {
    }

    public TimeRange(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return startTime + "-" + endTime;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof TimeRange) {
            if (this == other) {
                return true;
            }
            if (Objects.equals(startTime, ((TimeRange) other).startTime)) {
                return Objects.equals(endTime, ((TimeRange) other).endTime);
            }
        }
        return false;
    }
}
