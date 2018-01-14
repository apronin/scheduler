package com.apronin.scheduler;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;

public class Job implements Comparable<Job> {
    private LocalDateTime localDateTime;
    private Callable<Object> callable;

    public Job(LocalDateTime localDateTime, Callable<Object> callable) {
        this.localDateTime = localDateTime;
        this.callable = callable;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public Callable<Object> getCallable() {
        return callable;
    }

    @Override
    public int compareTo(Job another) {
        return localDateTime.compareTo(another.getLocalDateTime());
    }
}
