package de.dytanic.cloudnet.driver.template;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

@ToString
@EqualsAndHashCode
public class FileInfo implements SerializableObject {

    private String path;
    private String name;
    private boolean directory;
    private boolean hidden;
    private long creationTime;
    private long lastModified;
    private long lastAccess;
    private long size;

    public FileInfo(@NotNull String path, @NotNull String name, boolean directory, boolean hidden, long creationTime, long lastModified, long lastAccess, long size) {
        this.path = path;
        this.name = name;
        this.directory = directory;
        this.hidden = hidden;
        this.creationTime = creationTime;
        this.lastModified = lastModified;
        this.lastAccess = lastAccess;
        this.size = size;
    }

    public FileInfo() {
    }

    @NotNull
    public static FileInfo of(@NotNull Path path) throws IOException {
        return of(path, (Path) null);
    }

    @NotNull
    public static FileInfo of(@NotNull Path fullPath, @NotNull BasicFileAttributes attributes) throws IOException {
        return of(fullPath, null, attributes);
    }

    @NotNull
    public static FileInfo of(@NotNull Path path, @Nullable Path relativePath) throws IOException {
        return of(path, relativePath, Files.readAttributes(path, BasicFileAttributes.class));
    }

    @NotNull
    public static FileInfo of(@NotNull Path fullPath, @Nullable Path relativePath, @NotNull BasicFileAttributes attributes) throws IOException {
        if (relativePath == null) {
            relativePath = fullPath;
        }
        return new FileInfo(
                relativePath.toString().replace(File.separatorChar, '/'), relativePath.getFileName().toString(),
                attributes.isDirectory(), Files.isHidden(fullPath),
                attributes.creationTime().toMillis(), attributes.lastModifiedTime().toMillis(), attributes.lastAccessTime().toMillis(),
                attributes.size()
        );
    }

    @NotNull
    public static FileInfo of(@NotNull File file) throws IOException {
        return of(file.toPath(), file.toPath());
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
        this.size = buffer.readLong();
    }
}

