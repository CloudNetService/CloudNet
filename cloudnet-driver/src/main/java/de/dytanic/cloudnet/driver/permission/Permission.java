package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@ToString
@EqualsAndHashCode
public final class Permission implements SerializableObject {

    private String name;

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

    public Permission() {
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

    @Override
    public void write(ProtocolBuffer buffer) {
        buffer.writeString(this.name);
        buffer.writeInt(this.potency);
        buffer.writeLong(this.timeOutMillis);
    }

    @Override
    public void read(ProtocolBuffer buffer) {
        this.name = buffer.readString();
        this.potency = buffer.readInt();
        this.timeOutMillis = buffer.readLong();
    }
}