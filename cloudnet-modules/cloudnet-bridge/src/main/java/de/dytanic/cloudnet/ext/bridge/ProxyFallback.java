package de.dytanic.cloudnet.ext.bridge;

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

    public String getPermission() {
        return this.permission;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ProxyFallback)) return false;
        final ProxyFallback other = (ProxyFallback) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$task = this.getTask();
        final Object other$task = other.getTask();
        if (this$task == null ? other$task != null : !this$task.equals(other$task)) return false;
        final Object this$permission = this.getPermission();
        final Object other$permission = other.getPermission();
        if (this$permission == null ? other$permission != null : !this$permission.equals(other$permission))
            return false;
        if (this.getPriority() != other.getPriority()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ProxyFallback;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $task = this.getTask();
        result = result * PRIME + ($task == null ? 43 : $task.hashCode());
        final Object $permission = this.getPermission();
        result = result * PRIME + ($permission == null ? 43 : $permission.hashCode());
        result = result * PRIME + this.getPriority();
        return result;
    }

    public String toString() {
        return "ProxyFallback(task=" + this.getTask() + ", permission=" + this.getPermission() + ", priority=" + this.getPriority() + ")";
    }
}