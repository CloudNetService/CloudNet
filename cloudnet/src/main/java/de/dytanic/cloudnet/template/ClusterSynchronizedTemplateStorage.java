package de.dytanic.cloudnet.template;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.stream.MultiOutputStream;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedPacketBuilder;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.defaults.DefaultSyncTemplateStorage;
import de.dytanic.cloudnet.network.packet.PacketServerSyncTemplateStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Predicate;

public abstract class ClusterSynchronizedTemplateStorage extends DefaultSyncTemplateStorage {

    private boolean enabled = true;

    public void toggleSynchronization(boolean enabled) {
        this.enabled = enabled;
    }

    protected boolean requiresSynchronization() {
        return this.enabled && CloudNet.getInstance().getClusterNodeServerProvider().hasAnyConnection();
    }

    private void sendDefault(DriverAPIRequestType requestType, ServiceTemplate template, ProtocolBuffer buffer) {
        buffer.markWriterIndex();
        buffer.writerIndex(0);

        buffer.writeEnumConstant(requestType);
        buffer.writeObject(template);

        buffer.resetWriterIndex();

        CloudNet.getInstance().getClusterNodeServerProvider().sendPacket(new PacketServerSyncTemplateStorage(buffer));
    }

    private void sendChunks(DriverAPIRequestType requestType, InputStream inputStream, ServiceTemplate template, JsonDocument header) throws IOException {
        ChunkedPacketBuilder.newBuilder(PacketConstants.CLUSTER_TEMPLATE_STORAGE_CHUNK_SYNC_CHANNEL, inputStream)
                .header(header.append("type", requestType).append("template", template))
                .target(CloudNet.getInstance().getClusterNodeServerProvider().getConnectedChannels())
                .complete();
    }

    @Override
    public boolean deploy(@NotNull byte[] zipInput, @NotNull ServiceTemplate target) {
        if (this.deployWithoutSynchronization(zipInput, target)) {
            if (this.requiresSynchronization()) {
                this.sendDefault(DriverAPIRequestType.DEPLOY_TEMPLATE_BYTE_ARRAY, target, ProtocolBuffer.create().writeArray(zipInput));
            }

            return true;
        }

        return false;
    }

    public abstract boolean deployWithoutSynchronization(@NotNull byte[] zipInput, @NotNull ServiceTemplate target);

    @Override
    public boolean deploy(@NotNull File directory, @NotNull ServiceTemplate target, @Nullable Predicate<File> fileFilter) {
        if (this.deployWithoutSynchronization(directory, target, fileFilter)) {
            if (this.requiresSynchronization()) {
                try (InputStream inputStream = FileUtils.zipToStream(directory.toPath(), fileFilter != null ? path -> fileFilter.test(path.toFile()) : null)) {
                    this.sendChunks(DriverAPIRequestType.DEPLOY_TEMPLATE_STREAM, inputStream, target, JsonDocument.newDocument());
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }

            return true;
        }

        return false;
    }

    public abstract boolean deployWithoutSynchronization(@NotNull File directory, @NotNull ServiceTemplate target, @Nullable Predicate<File> fileFilter);

    @Override
    public boolean deploy(@NotNull InputStream inputStream, @NotNull ServiceTemplate target) {
        if (!this.requiresSynchronization()) {
            return this.deployWithoutSynchronization(inputStream, target);
        }

        Path tempFile = FileUtils.createTempFile();
        try {
            Files.copy(inputStream, tempFile);

            try (InputStream localInputStream = Files.newInputStream(tempFile)) {
                if (this.deployWithoutSynchronization(localInputStream, target)) {

                    try (InputStream remoteInputStream = Files.newInputStream(tempFile, StandardOpenOption.DELETE_ON_CLOSE)) {
                        this.sendChunks(DriverAPIRequestType.DEPLOY_TEMPLATE_STREAM, remoteInputStream, target, JsonDocument.newDocument());
                    }

                    return true;
                }

                Files.delete(tempFile);
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    public abstract boolean deployWithoutSynchronization(@NotNull InputStream inputStream, @NotNull ServiceTemplate target);

    @Override
    public boolean delete(@NotNull ServiceTemplate template) {
        if (this.deleteWithoutSynchronization(template)) {
            if (this.requiresSynchronization()) {
                this.sendDefault(DriverAPIRequestType.DELETE_TEMPLATE, template, ProtocolBuffer.create());
            }

            return true;
        }

        return false;
    }

    public abstract boolean deleteWithoutSynchronization(@NotNull ServiceTemplate template);

    @Override
    public boolean create(@NotNull ServiceTemplate template) {
        if (this.createWithoutSynchronization(template)) {
            if (this.requiresSynchronization()) {
                this.sendDefault(DriverAPIRequestType.CREATE_TEMPLATE, template, ProtocolBuffer.create());
            }

            return true;
        }

        return false;
    }

    public abstract boolean createWithoutSynchronization(@NotNull ServiceTemplate template);

    @Override
    public @Nullable OutputStream appendOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.wrapOutputStream(DriverAPIRequestType.APPEND_FILE_CONTENT, template, path, this.appendOutputStreamWithoutSynchronization(template, path));
    }

    public abstract OutputStream appendOutputStreamWithoutSynchronization(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

    @Override
    public @Nullable OutputStream newOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.wrapOutputStream(DriverAPIRequestType.SET_FILE_CONTENT, template, path, this.newOutputStreamWithoutSynchronization(template, path));
    }

    public abstract OutputStream newOutputStreamWithoutSynchronization(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

    private OutputStream wrapOutputStream(@NotNull DriverAPIRequestType requestType, @NotNull ServiceTemplate template, @NotNull String path, @Nullable OutputStream outputStream) throws IOException {
        if (outputStream == null || !this.requiresSynchronization()) {
            return outputStream;
        }

        Path tempFile = FileUtils.createTempFile();

        return new MultiOutputStream(Files.newOutputStream(tempFile), outputStream) {
            @Override
            public void close() throws IOException {
                super.close();
                try (InputStream inputStream = Files.newInputStream(tempFile, StandardOpenOption.DELETE_ON_CLOSE)) {
                    sendChunks(requestType, inputStream, template, JsonDocument.newDocument("path", path));
                }
            }
        };
    }

    @Override
    public boolean createFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        if (this.createFileWithoutSynchronization(template, path)) {
            if (this.requiresSynchronization()) {
                this.sendDefault(DriverAPIRequestType.CREATE_FILE, template, ProtocolBuffer.create().writeString(path));
            }

            return true;
        }

        return false;
    }

    public abstract boolean createFileWithoutSynchronization(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

    @Override
    public boolean createDirectory(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        if (this.createDirectoryWithoutSynchronization(template, path)) {
            if (this.requiresSynchronization()) {
                this.sendDefault(DriverAPIRequestType.CREATE_DIRECTORY, template, ProtocolBuffer.create().writeString(path));
            }

            return true;
        }

        return false;
    }

    public abstract boolean createDirectoryWithoutSynchronization(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

    @Override
    public boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        if (this.deleteFileWithoutSynchronization(template, path)) {
            if (this.requiresSynchronization()) {
                this.sendDefault(DriverAPIRequestType.DELETE_FILE, template, ProtocolBuffer.create().writeString(path));
            }

            return true;
        }

        return false;
    }

    public abstract boolean deleteFileWithoutSynchronization(@NotNull ServiceTemplate template, @NotNull String path) throws IOException;

}
