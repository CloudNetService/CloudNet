package de.dytanic.cloudnet.driver.template;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class FileInfo implements SerializableObject {

    private String path;
    private String name;
    private boolean directory;
    private boolean hidden;
    private long creationTime;
    private long lastModified;
    private long lastAccess;
    private boolean readable;
    private boolean writable;
    private long size;

    public FileInfo(@NotNull String path, @NotNull String name, boolean directory, boolean hidden, long creationTime, long lastModified, long lastAccess, boolean readable, boolean writable, long size) {
        this.path = path;
        this.name = name;
        this.directory = directory;
        this.hidden = hidden;
        this.creationTime = creationTime;
        this.lastModified = lastModified;
        this.lastAccess = lastAccess;
        this.readable = readable;
        this.writable = writable;
        this.size = size;
    }

    @NotNull
    public static FileInfo of(@NotNull Path path) throws IOException {
        return of(path, Files.readAttributes(path, BasicFileAttributes.class));
    }

    @NotNull
    public static FileInfo of(@NotNull Path path, @NotNull BasicFileAttributes attributes) throws IOException {
        return new FileInfo(
                path.toString(), path.getFileName().toString(),
                attributes.isDirectory(), Files.isHidden(path),
                attributes.creationTime().toMillis(), attributes.lastModifiedTime().toMillis(), attributes.lastAccessTime().toMillis(),
                Files.isReadable(path), Files.isWritable(path),
                attributes.size()
        );
    }

    @NotNull
    public static FileInfo of(@NotNull File file) throws IOException {
        return of(file.toPath());
    }

    @NotNull
    public String getPath() {
        return this.path;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public boolean isDirectory() {
        return this.directory;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public long getLastModified() {
        return this.lastModified;
    }

    public long getLastAccess() {
        return this.lastAccess;
    }

    public boolean isReadable() {
        return this.readable;
    }

    public boolean isWritable() {
        return this.writable;
    }

    public long getSize() {
        return this.size;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeString(this.path);
        buffer.writeString(this.name);
        buffer.writeBoolean(this.directory);
        buffer.writeBoolean(this.hidden);
        buffer.writeLong(this.creationTime);
        buffer.writeLong(this.lastModified);
        buffer.writeLong(this.lastAccess);
        buffer.writeBoolean(this.readable);
        buffer.writeBoolean(this.writable);
        buffer.writeLong(this.size);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.path = buffer.readString();
        this.name = buffer.readString();
        this.directory = buffer.readBoolean();
        this.hidden = buffer.readBoolean();
        this.creationTime = buffer.readLong();
        this.lastModified = buffer.readLong();
        this.lastAccess = buffer.readLong();
        this.readable = buffer.readBoolean();
        this.writable = buffer.readBoolean();
        this.size = buffer.readLong();
    }
}
