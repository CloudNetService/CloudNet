package de.dytanic.cloudnet.driver.service;

public class ThreadSnapshot {

    private long id;

    private String name;

    private Thread.State threadState;

    private boolean daemon;

    private int priority;

    public ThreadSnapshot(long id, String name, Thread.State threadState, boolean daemon, int priority) {
        this.id = id;
        this.name = name;
        this.threadState = threadState;
        this.daemon = daemon;
        this.priority = priority;
    }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Thread.State getThreadState() {
        return this.threadState;
    }

    public boolean isDaemon() {
        return this.daemon;
    }

    public int getPriority() {
        return this.priority;
    }
}