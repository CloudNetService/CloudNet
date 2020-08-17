package de.dytanic.cloudnet.driver.template;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

public class DefaultSpecificTemplateStorage implements SpecificTemplateStorage {

    private final ServiceTemplate template;
    private final TemplateStorage storage;

    private DefaultSpecificTemplateStorage(@NotNull ServiceTemplate template, @NotNull TemplateStorage storage) {
        this.template = template;
        this.storage = storage;
    }

    public static DefaultSpecificTemplateStorage of(@NotNull ServiceTemplate template, @NotNull TemplateStorage storage) {
        if (!storage.getName().equals(template.getStorage())) {
            throw new IllegalArgumentException(String.format("Storage '%s' doesn't match the storage of the template ('%s')", storage.getName(), template.getStorage()));
        }
        return new DefaultSpecificTemplateStorage(template, storage);
    }

    public static DefaultSpecificTemplateStorage of(@NotNull ServiceTemplate template) {
        TemplateStorage storage = CloudNetDriver.getInstance().getTemplateStorage(template.getStorage());
        if (storage == null) {
            throw new IllegalArgumentException(String.format("Storage '%s' not found", template.getStorage()));
        }
        return new DefaultSpecificTemplateStorage(template, storage);
    }

    @Override
    public String getName() {
        return this.storage.getName();
    }

    @Override
    public @NotNull ServiceTemplate getTargetTemplate() {
        return this.template;
    }

    @Override
    public @NotNull TemplateStorage getWrappedStorage() {
        return this.storage;
    }

    @Override
    public boolean deploy(@NotNull byte[] zipInput) {
        return this.storage.deploy(zipInput, this.template);
    }

    @Override
    public boolean deploy(@NotNull File directory, @Nullable Predicate<File> fileFilter) {
        return this.storage.deploy(directory, this.template, fileFilter);
    }

    @Override
    public boolean deploy(@NotNull InputStream inputStream) {
        return this.storage.deploy(inputStream, this.template);
    }

    @Override
    public boolean copy(@NotNull File directory) {
        return this.storage.copy(this.template, directory);
    }

    @Override
    public boolean copy(@NotNull Path directory) {
        return this.storage.copy(this.template, directory);
    }

    @Override
    public byte[] toZipByteArray() {
        return this.storage.toZipByteArray(this.template);
    }

    @Override
    public @Nullable ZipInputStream asZipInputStream() throws IOException {
        return this.storage.asZipInputStream(this.template);
    }

    @Override
    public @Nullable InputStream zipTemplate() throws IOException {
        return this.storage.zipTemplate(this.template);
    }

    @Override
    public boolean delete() {
        return this.storage.delete(this.template);
    }

    @Override
    public boolean create() {
        return this.storage.create(this.template);
    }

    @Override
    public boolean exists() {
        return this.storage.has(this.template);
    }

    @Override
    public @Nullable OutputStream appendOutputStream(@NotNull String path) throws IOException {
        return this.storage.appendOutputStream(this.template, path);
    }

    @Override
    public @Nullable OutputStream newOutputStream(@NotNull String path) throws IOException {
        return this.storage.newOutputStream(this.template, path);
    }

    @Override
    public boolean createFile(@NotNull String path) throws IOException {
        return this.storage.createFile(this.template, path);
    }

    @Override
    public boolean createDirectory(@NotNull String path) throws IOException {
        return this.storage.createDirectory(this.template, path);
    }

    @Override
    public boolean hasFile(@NotNull String path) throws IOException {
        return this.storage.hasFile(this.template, path);
    }

    @Override
    public boolean deleteFile(@NotNull String path) throws IOException {
        return this.storage.deleteFile(this.template, path);
    }

    @Override
    public @Nullable InputStream newInputStream(@NotNull String path) throws IOException {
        return this.storage.newInputStream(this.template, path);
    }

    @Override
    public @Nullable FileInfo getFileInfo(@NotNull String path) throws IOException {
        return this.storage.getFileInfo(this.template, path);
    }

    @Override
    public FileInfo[] listFiles(@NotNull String dir, boolean deep) throws IOException {
        return this.storage.listFiles(this.template, dir, deep);
    }

    @Override
    public FileInfo[] listFiles(boolean deep) throws IOException {
        return this.storage.listFiles(this.template, deep);
    }

    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull byte[] zipInput) {
        return this.storage.deployAsync(zipInput, this.template);
    }

    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull File directory, @Nullable Predicate<File> fileFilter) {
        return this.storage.deployAsync(directory, this.template, fileFilter);
    }

    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull InputStream inputStream) {
        return this.storage.deployAsync(inputStream, this.template);
    }

    @Override
    public @NotNull ITask<Boolean> copyAsync(@NotNull File directory) {
        return this.storage.deployAsync(directory, this.template);
    }

    @Override
    public @NotNull ITask<Boolean> copyAsync(@NotNull Path directory) {
        return this.storage.copyAsync(this.template, directory);
    }

    @Override
    public @NotNull ITask<byte[]> toZipByteArrayAsync() {
        return this.storage.toZipByteArrayAsync(this.template);
    }

    @Override
    public @NotNull ITask<ZipInputStream> asZipInputStreamAsync() {
        return this.storage.asZipInputStreamAsync(this.template);
    }

    @Override
    public @NotNull ITask<InputStream> zipTemplateAsync() {
        return this.storage.zipTemplateAsync(this.template);
    }

    @Override
    public @NotNull ITask<Boolean> deleteAsync() {
        return this.storage.deleteAsync(this.template);
    }

    @Override
    public @NotNull ITask<Boolean> createAsync() {
        return this.storage.createAsync(this.template);
    }

    @Override
    public @NotNull ITask<Boolean> existsAsync() {
        return this.storage.hasAsync(this.template);
    }

    @Override
    public @NotNull ITask<OutputStream> appendOutputStreamAsync(@NotNull String path) {
        return this.storage.appendOutputStreamAsync(this.template, path);
    }

    @Override
    public @NotNull ITask<OutputStream> newOutputStreamAsync(@NotNull String path) {
        return this.storage.newOutputStreamAsync(this.template, path);
    }

    @Override
    public @NotNull ITask<Boolean> createFileAsync(@NotNull String path) {
        return this.storage.createFileAsync(this.template, path);
    }

    @Override
    public @NotNull ITask<Boolean> createDirectoryAsync(@NotNull String path) {
        return this.storage.createDirectoryAsync(this.template, path);
    }

    @Override
    public @NotNull ITask<Boolean> hasFileAsync(@NotNull String path) {
        return this.storage.hasFileAsync(this.template, path);
    }

    @Override
    public @NotNull ITask<Boolean> deleteFileAsync(@NotNull String path) {
        return this.storage.deleteFileAsync(this.template, path);
    }

    @Override
    public @NotNull ITask<InputStream> newInputStreamAsync(@NotNull String path) {
        return this.storage.newInputStreamAsync(this.template, path);
    }

    @Override
    public @NotNull ITask<FileInfo> getFileInfoAsync(@NotNull String path) {
        return this.storage.getFileInfoAsync(this.template, path);
    }

    @Override
    public @NotNull ITask<FileInfo[]> listFilesAsync(@NotNull String dir, boolean deep) {
        return this.storage.listFilesAsync(this.template, dir, deep);
    }

    @Override
    public @NotNull ITask<FileInfo[]> listFilesAsync(boolean deep) {
        return this.storage.listFilesAsync(this.template, deep);
    }
}
