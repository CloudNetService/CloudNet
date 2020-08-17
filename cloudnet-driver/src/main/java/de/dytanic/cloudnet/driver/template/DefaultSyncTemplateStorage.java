package de.dytanic.cloudnet.driver.template;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

public abstract class DefaultSyncTemplateStorage implements TemplateStorage {

    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull byte[] zipInput, @NotNull ServiceTemplate target) {
        return CompletableTask.supplyAsync(() -> this.deploy(zipInput, target));
    }

    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull File directory, @NotNull ServiceTemplate target, @Nullable Predicate<File> fileFilter) {
        return CompletableTask.supplyAsync(() -> this.deploy(directory, target, fileFilter));
    }

    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull File directory, @NotNull ServiceTemplate target) {
        return CompletableTask.supplyAsync(() -> this.deploy(directory, target));
    }

    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull InputStream inputStream, @NotNull ServiceTemplate target) {
        return CompletableTask.supplyAsync(() -> this.deploy(inputStream, target));
    }

    @Override
    public @NotNull ITask<Boolean> copyAsync(@NotNull ServiceTemplate template, @NotNull File directory) {
        return CompletableTask.supplyAsync(() -> this.copy(template, directory));
    }

    @Override
    public @NotNull ITask<Boolean> copyAsync(@NotNull ServiceTemplate template, @NotNull Path directory) {
        return CompletableTask.supplyAsync(() -> this.copy(template, directory));
    }

    @Override
    public @NotNull ITask<byte[]> toZipByteArrayAsync(@NotNull ServiceTemplate template) {
        return CompletableTask.supplyAsync(() -> this.toZipByteArray(template));
    }

    @Override
    public @NotNull ITask<ZipInputStream> asZipInputStreamAsync(@NotNull ServiceTemplate template) {
        return CompletableTask.supplyAsync(() -> this.asZipInputStream(template));
    }

    @Override
    public @NotNull ITask<InputStream> zipTemplateAsync(@NotNull ServiceTemplate template) {
        return CompletableTask.supplyAsync(() -> this.zipTemplate(template));
    }

    @Override
    public @NotNull ITask<Boolean> deleteAsync(@NotNull ServiceTemplate template) {
        return CompletableTask.supplyAsync(() -> this.delete(template));
    }

    @Override
    public @NotNull ITask<Boolean> createAsync(@NotNull ServiceTemplate template) {
        return CompletableTask.supplyAsync(() -> this.create(template));
    }

    @Override
    public @NotNull ITask<Boolean> hasAsync(@NotNull ServiceTemplate template) {
        return CompletableTask.supplyAsync(() -> this.has(template));
    }

    @Override
    public @NotNull ITask<OutputStream> appendOutputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        return CompletableTask.supplyAsync(() -> this.appendOutputStream(template, path));
    }

    @Override
    public @NotNull ITask<OutputStream> newOutputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        return CompletableTask.supplyAsync(() -> this.newOutputStream(template, path));
    }

    @Override
    public @NotNull ITask<Boolean> createFileAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        return CompletableTask.supplyAsync(() -> this.createFile(template, path));
    }

    @Override
    public @NotNull ITask<Boolean> createDirectoryAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        return CompletableTask.supplyAsync(() -> this.createDirectory(template, path));
    }

    @Override
    public @NotNull ITask<Boolean> hasFileAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        return CompletableTask.supplyAsync(() -> this.hasFile(template, path));
    }

    @Override
    public @NotNull ITask<Boolean> deleteFileAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        return CompletableTask.supplyAsync(() -> this.deleteFile(template, path));
    }

    @Override
    public @NotNull ITask<InputStream> newInputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        return CompletableTask.supplyAsync(() -> this.newInputStream(template, path));
    }

    @Override
    public @NotNull ITask<FileInfo> getFileInfoAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        return CompletableTask.supplyAsync(() -> this.getFileInfo(template, path));
    }

    @Override
    public @NotNull ITask<FileInfo[]> listFilesAsync(@NotNull ServiceTemplate template, @NotNull String dir, boolean deep) {
        return CompletableTask.supplyAsync(() -> this.listFiles(template, dir, deep));
    }

    @Override
    public @NotNull ITask<FileInfo[]> listFilesAsync(@NotNull ServiceTemplate template, boolean deep) {
        return CompletableTask.supplyAsync(() -> this.listFiles(template, deep));
    }

    @Override
    public @NotNull ITask<Collection<ServiceTemplate>> getTemplatesAsync() {
        return CompletableTask.supplyAsync(this::getTemplates);
    }

    @Override
    public @NotNull ITask<Void> closeAsync() {
        return CompletableTask.supplyAsync(() -> {
            this.close();
            return null;
        });
    }

}
