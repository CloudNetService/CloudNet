package de.dytanic.cloudnet.driver.template.defaults;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

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
    public boolean copy(@NotNull ServiceTemplate template, @NotNull File directory) {
        return this.copyAsync(template, directory).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory) {
        return this.copyAsync(template, directory).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    public byte[] toZipByteArray(@NotNull ServiceTemplate template) {
        return this.toZipByteArrayAsync(template).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public @Nullable ZipInputStream asZipInputStream(@NotNull ServiceTemplate template) throws IOException {
        return this.catchIOException(this.asZipInputStreamAsync(template), null);
    }

    @Override
    public @Nullable InputStream zipTemplate(@NotNull ServiceTemplate template) throws IOException {
        return this.catchIOException(this.zipTemplateAsync(template), null);
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
    public @Nullable OutputStream appendOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.catchIOException(this.appendOutputStreamAsync(template, path), null);
    }

    @Override
    public @Nullable OutputStream newOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.catchIOException(this.newOutputStreamAsync(template, path), null);
    }

    @Override
    public boolean createFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.catchIOException(this.createFileAsync(template, path), false);
    }

    @Override
    public boolean createDirectory(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.catchIOException(this.createDirectoryAsync(template, path), false);
    }

    @Override
    public boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.catchIOException(this.hasFileAsync(template, path), false);
    }

    @Override
    public boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.catchIOException(this.deleteFileAsync(template, path), false);
    }

    @Override
    public @Nullable InputStream newInputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.catchIOException(this.newInputStreamAsync(template, path), null);
    }

    @Override
    public @Nullable FileInfo getFileInfo(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.catchIOException(this.getFileInfoAsync(template, path), null);
    }

    @Override
    public FileInfo[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir, boolean deep) throws IOException {
        return this.catchIOException(this.listFilesAsync(template, dir, deep), null);
    }

    @Override
    public FileInfo[] listFiles(@NotNull ServiceTemplate template, boolean deep) throws IOException {
        return this.catchIOException(this.listFilesAsync(template, deep), null);
    }

    @Override
    public @NotNull Collection<ServiceTemplate> getTemplates() {
        return this.getTemplatesAsync().get(5, TimeUnit.SECONDS, Collections.emptyList());
    }

    @Override
    public void close() throws IOException {
        this.catchIOException(this.closeAsync(), null);
    }

    private <V> V catchIOException(ITask<V> task, V def) throws IOException {
        try {
            return task.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException exception) {
            exception.printStackTrace();
            return def;
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof IOException) {
                throw (IOException) exception.getCause();
            }
        }
        return def;
    }

}
