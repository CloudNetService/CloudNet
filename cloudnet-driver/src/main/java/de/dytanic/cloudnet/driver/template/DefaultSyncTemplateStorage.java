package de.dytanic.cloudnet.driver.template;

import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

// TODO this should be async, not with the CompletedTask
public abstract class DefaultSyncTemplateStorage implements TemplateStorage {
    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull byte[] zipInput, @NotNull ServiceTemplate target) {
        return CompletedTask.create(this.deploy(zipInput, target));
    }

    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull File directory, @NotNull ServiceTemplate target, @Nullable Predicate<File> fileFilter) {
        return CompletedTask.create(this.deploy(directory, target, fileFilter));
    }

    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull File directory, @NotNull ServiceTemplate target) {
        return CompletedTask.create(this.deploy(directory, target));
    }

    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull ZipInputStream inputStream, @NotNull ServiceTemplate target) {
        return CompletedTask.create(this.deploy(inputStream, target));
    }

    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull Path[] paths, @NotNull ServiceTemplate target) {
        return CompletedTask.create(this.deploy(paths, target));
    }

    @Override
    public @NotNull ITask<Boolean> deployAsync(@NotNull File[] files, @NotNull ServiceTemplate target) {
        return CompletedTask.create(this.deploy(files, target));
    }

    @Override
    public @NotNull ITask<Boolean> copyAsync(@NotNull ServiceTemplate template, @NotNull File directory) {
        return CompletedTask.create(this.copy(template, directory));
    }

    @Override
    public @NotNull ITask<Boolean> copyAsync(@NotNull ServiceTemplate template, @NotNull Path directory) {
        return CompletedTask.create(this.copy(template, directory));
    }

    @Override
    public @NotNull ITask<Boolean> copyAsync(@NotNull ServiceTemplate template, @NotNull File[] directories) {
        return CompletedTask.create(this.copy(template, directories));
    }

    @Override
    public @NotNull ITask<Boolean> copyAsync(@NotNull ServiceTemplate template, @NotNull Path[] directories) {
        return CompletedTask.create(this.copy(template, directories));
    }

    @Override
    public @NotNull ITask<byte[]> toZipByteArrayAsync(@NotNull ServiceTemplate template) {
        return CompletedTask.create(this.toZipByteArray(template));
    }

    @Override
    public @NotNull ITask<ZipInputStream> asZipInputStreamAsync(@NotNull ServiceTemplate template) {
        try {
            return CompletedTask.create(this.asZipInputStream(template));
        } catch (IOException exception) {
            return CompletedTask.createFailed(exception);
        }
    }

    @Override
    public @NotNull ITask<Boolean> deleteAsync(@NotNull ServiceTemplate template) {
        return CompletedTask.create(this.delete(template));
    }

    @Override
    public @NotNull ITask<Boolean> createAsync(@NotNull ServiceTemplate template) {
        return CompletedTask.create(this.create(template));
    }

    @Override
    public @NotNull ITask<Boolean> hasAsync(@NotNull ServiceTemplate template) {
        return CompletedTask.create(this.has(template));
    }

    @Override
    public @NotNull ITask<OutputStream> appendOutputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        try {
            return CompletedTask.create(this.appendOutputStream(template, path));
        } catch (IOException exception) {
            return CompletedTask.createFailed(exception);
        }
    }

    @Override
    public @NotNull ITask<OutputStream> newOutputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        try {
            return CompletedTask.create(this.newOutputStream(template, path));
        } catch (IOException exception) {
            return CompletedTask.createFailed(exception);
        }
    }

    @Override
    public @NotNull ITask<Boolean> createFileAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        try {
            return CompletedTask.create(this.createFile(template, path));
        } catch (IOException exception) {
            return CompletedTask.createFailed(exception);
        }
    }

    @Override
    public @NotNull ITask<Boolean> createDirectoryAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        try {
            return CompletedTask.create(this.createDirectory(template, path));
        } catch (IOException exception) {
            return CompletedTask.createFailed(exception);
        }
    }

    @Override
    public @NotNull ITask<Boolean> hasFileAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        try {
            return CompletedTask.create(this.hasFile(template, path));
        } catch (IOException exception) {
            return CompletedTask.createFailed(exception);
        }
    }

    @Override
    public @NotNull ITask<Boolean> deleteFileAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        try {
            return CompletedTask.create(this.deleteFile(template, path));
        } catch (IOException exception) {
            return CompletedTask.createFailed(exception);
        }
    }

    @Override
    public @NotNull ITask<InputStream> newInputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        try {
            return CompletedTask.create(this.newInputStream(template, path));
        } catch (IOException exception) {
            return CompletedTask.createFailed(exception);
        }
    }

    @Override
    public @NotNull ITask<FileInfo> getFileInfoAsync(@NotNull ServiceTemplate template, @NotNull String path) {
        try {
            return CompletedTask.create(this.getFileInfo(template, path));
        } catch (IOException exception) {
            return CompletedTask.createFailed(exception);
        }
    }

    @Override
    public @NotNull ITask<FileInfo[]> listFilesAsync(@NotNull ServiceTemplate template, @NotNull String dir) {
        try {
            return CompletedTask.create(this.listFiles(template, dir));
        } catch (IOException exception) {
            return CompletedTask.createFailed(exception);
        }
    }

    @Override
    public @NotNull ITask<FileInfo[]> listFilesAsync(@NotNull ServiceTemplate template) {
        try {
            return CompletedTask.create(this.listFiles(template));
        } catch (IOException exception) {
            return CompletedTask.createFailed(exception);
        }
    }

    @Override
    public @NotNull ITask<Collection<ServiceTemplate>> getTemplatesAsync() {
        return CompletedTask.create(this.getTemplates());
    }

    @Override
    public @NotNull ITask<Void> closeAsync() {
        try {
            this.close();
            return CompletedTask.voidTask();
        } catch (IOException exception) {
            return CompletedTask.createFailed(exception);
        }
    }

}
