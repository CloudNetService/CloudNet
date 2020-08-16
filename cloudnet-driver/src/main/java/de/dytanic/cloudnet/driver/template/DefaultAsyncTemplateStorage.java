package de.dytanic.cloudnet.driver.template;

import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

// TODO IOExceptions out of the tasks should be thrown if present
public abstract class DefaultAsyncTemplateStorage implements TemplateStorage {
    @Override
    public boolean deploy(@NotNull byte[] zipInput, @NotNull ServiceTemplate target) {
        return this.deployAsync(zipInput, target).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean deploy(@NotNull File directory, @NotNull ServiceTemplate target, @Nullable Predicate<File> fileFilter) {
        return this.deployAsync(directory, target, fileFilter).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean deploy(@NotNull File directory, @NotNull ServiceTemplate target) {
        return this.deployAsync(directory, target).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean deploy(@NotNull InputStream inputStream, @NotNull ServiceTemplate target) {
        return this.deployAsync(inputStream, target).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean deploy(@NotNull Path[] paths, @NotNull ServiceTemplate target) {
        return this.deployAsync(paths, target).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean deploy(@NotNull File[] files, @NotNull ServiceTemplate target) {
        return this.deployAsync(files, target).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull File directory) {
        return this.copyAsync(template, directory).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory) {
        return this.copyAsync(template, directory).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull File[] directories) {
        return this.copyAsync(template, directories).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull Path[] directories) {
        return this.copyAsync(template, directories).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public byte[] toZipByteArray(@NotNull ServiceTemplate template) {
        return this.toZipByteArrayAsync(template).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public @Nullable ZipInputStream asZipInputStream(@NotNull ServiceTemplate template) {
        return this.asZipInputStreamAsync(template).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public @Nullable InputStream zipTemplate(@NotNull ServiceTemplate template) {
        return this.zipTemplateAsync(template).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public boolean delete(@NotNull ServiceTemplate template) {
        return this.deleteAsync(template).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean create(@NotNull ServiceTemplate template) {
        return this.createAsync(template).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean has(@NotNull ServiceTemplate template) {
        return this.hasAsync(template).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public @Nullable OutputStream appendOutputStream(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.appendOutputStreamAsync(template, path).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public @Nullable OutputStream newOutputStream(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.newOutputStreamAsync(template, path).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public boolean createFile(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.createFileAsync(template, path).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean createDirectory(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.createDirectoryAsync(template, path).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.hasFileAsync(template, path).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.deleteFileAsync(template, path).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public @Nullable InputStream newInputStream(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.newInputStreamAsync(template, path).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public @Nullable FileInfo getFileInfo(@NotNull ServiceTemplate template, @NotNull String path) {
        return this.getFileInfoAsync(template, path).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public FileInfo[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir, boolean deep) {
        return this.listFilesAsync(template, dir, deep).get(5, TimeUnit.SECONDS, new FileInfo[0]);
    }

    @Override
    public FileInfo[] listFiles(@NotNull ServiceTemplate template, boolean deep) {
        return this.listFilesAsync(template, deep).get(5, TimeUnit.SECONDS, new FileInfo[0]);
    }

    @Override
    public @NotNull Collection<ServiceTemplate> getTemplates() {
        return this.getTemplatesAsync().get(5, TimeUnit.SECONDS, Collections.emptyList());
    }

    @Override
    public void close() {
        this.closeAsync().get(5, TimeUnit.SECONDS, null);
    }

}
