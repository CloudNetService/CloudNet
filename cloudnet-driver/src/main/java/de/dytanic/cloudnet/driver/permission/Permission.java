package de.dytanic.cloudnet.driver.permission;

import java.util.concurrent.TimeUnit;

public final class Permission {

    private final String name;

    private int potency;

    private long timeOutMillis;

    public Permission(String name, int potency) {
        this.name = name;
        this.potency = potency;
    }

    public Permission(String name, int potency, long time, TimeUnit timeUnit) {
        this.name = name;
        this.potency = potency;
        this.timeOutMillis = System.currentTimeMillis() + timeUnit.toMillis(time);
    }

    public Permission(String name) {
        this.name = name;
    }

    public Permission(String name, int potency, long timeOutMillis) {
        this.name = name;
        this.potency = potency;
        this.timeOutMillis = timeOutMillis;
    }

    public String getName() {
        return this.name;
    }

    public int getPotency() {
        return this.potency;
    }

    public long getTimeOutMillis() {
        return this.timeOutMillis;
    }

    public void setPotency(int potency) {
        this.potency = potency;
    }

    public void setTimeOutMillis(long timeOutMillis) {
        this.timeOutMillis = timeOutMillis;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Permission)) return false;
        final Permission other = (Permission) o;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        if (this.getPotency() != other.getPotency()) return false;
        if (this.getTimeOutMillis() != other.getTimeOutMillis()) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        result = result * PRIME + this.getPotency();
        final long $timeOutMillis = this.getTimeOutMillis();
        result = result * PRIME + (int) ($timeOutMillis >>> 32 ^ $timeOutMillis);
        return result;
    }

    public String toString() {
        return "Permission(name=" + this.getName() + ", potency=" + this.getPotency() + ", timeOutMillis=" + this.getTimeOutMillis() + ")";
    }
}