package de.dytanic.cloudnet.driver.permission;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@ToString
@EqualsAndHashCode
public final class Permission {

    private final String name;

    private int potency;

    private long timeOutMillis;

    public Permission(@NotNull String name, int potency) {
        this.name = name;
        this.potency = potency;
    }

    public Permission(@NotNull String name, int potency, long time, @NotNull TimeUnit timeUnit) {
        this.name = name;
        this.potency = potency;
        this.timeOutMillis = System.currentTimeMillis() + timeUnit.toMillis(time);
    }

    public Permission(@NotNull String name) {
        this.name = name;
    }

    public Permission(@NotNull String name, int potency, long timeOutMillis) {
        this.name = name;
        this.potency = potency;
        this.timeOutMillis = timeOutMillis;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public int getPotency() {
        return this.potency;
    }

    public void setPotency(int potency) {
        this.potency = potency;
    }

    public long getTimeOutMillis() {
        return this.timeOutMillis;
    }

    public void setTimeOutMillis(long timeOutMillis) {
        this.timeOutMillis = timeOutMillis;
    }

}