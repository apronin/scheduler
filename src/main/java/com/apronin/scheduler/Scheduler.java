package com.apronin.scheduler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Scheduler {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Object lock = new Object();
    private volatile boolean isRunning = true;
    private Logger logger = Logger.getLogger(Scheduler.class.getName());
    private TreeMap<LocalDateTime, LinkedList<Callable<Object>>> queue = new TreeMap<>();

    public Scheduler() {
        Thread workingThread = new Thread(this::loop);
        workingThread.start();
    }

    public void schedule(LocalDateTime localDateTime, Callable<Object> callable) {
        Objects.requireNonNull(localDateTime, "localDateTime is null");
        Objects.requireNonNull(callable, "callable is null");
        logger.info("schedule");
        if (!isRunning) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.compareTo(localDateTime) >= 0) {
            logger.info("execute callable");
            executor.submit(callable);
        } else {
            synchronized (lock) {
                logger.info("add to pool");
                addNewElementToQueue(localDateTime, callable);
                lock.notify();
            }
        }
    }

    private void addNewElementToQueue(LocalDateTime localDateTime, Callable<Object> callable){
        if(queue.containsKey(localDateTime)){
            LinkedList<Callable<Object>> items = queue.get(localDateTime);
            items.add(callable);
        } else {
            LinkedList<Callable<Object>> items = new LinkedList<>();
            items.add(callable);
            queue.put(localDateTime, items);
        }
    }

    private Map.Entry<LocalDateTime, LinkedList<Callable<Object>>> getFirstElement(){
        return queue.firstEntry();
    }

    private void loop() {
        while (isRunning) {
            synchronized (lock) {
                try {
                    Map.Entry<LocalDateTime, LinkedList<Callable<Object>>> job = getFirstElement();
                    if (job == null) {
                        logger.info("pool is empty");
                        lock.wait();
                    } else {
                        LocalDateTime now = LocalDateTime.now();
                        if (now.compareTo(job.getKey()) >= 0) {
                            logger.info("submit right now");
                            for (Callable<Object> objectCallable : job.getValue()) {
                                executor.submit(objectCallable);
                            }
                            queue.remove(job.getKey());
                        } else {
                            long until = now.until(job.getKey(), ChronoUnit.MILLIS);
                            logger.info("sleep some time: " + until);
                            lock.wait(until);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stopGracefully() {
        logger.info("stop scheduler");
        isRunning = false;
        synchronized (lock) {
            lock.notify();
        }
        executor.shutdown();
    }
}
