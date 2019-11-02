package de.dytanic.cloudnet.ext.bridge;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class ProxyFallback implements Comparable<ProxyFallback> {

    protected String task, permission;
    private int priority;

    public ProxyFallback(String task, String permission, int priority) {
        this.task = task;
        this.permission = permission;
        this.priority = priority;
    }

    public ProxyFallback() {
    }

    @Override
    public int compareTo(ProxyFallback o) {
        return priority + o.priority;
    }

    public String getTask() {
        return this.task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getPermission() {
        return this.permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

}