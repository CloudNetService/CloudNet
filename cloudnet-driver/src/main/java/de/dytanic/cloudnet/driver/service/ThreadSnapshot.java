package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
/**
 * The information of a thread running on any process in the Cloud.
 */
public class ThreadSnapshot implements SerializableObject {

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

    public ThreadSnapshot() {
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

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeLong(this.id);
        buffer.writeString(this.name);
        buffer.writeEnumConstant(this.threadState);
        buffer.writeBoolean(this.daemon);
        buffer.writeVarInt(this.priority);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.id = buffer.readLong();
        this.name = buffer.readString();
        this.threadState = buffer.readEnumConstant(Thread.State.class);
        this.daemon = buffer.readBoolean();
        this.priority = buffer.readVarInt();
    }
}