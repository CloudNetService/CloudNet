package de.dytanic.cloudnet.ext.storage.ftp.storage.queue;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.storage.ftp.storage.AbstractFTPStorage;
import de.dytanic.cloudnet.template.ITemplateStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

public class FTPQueueStorage implements Runnable, ITemplateStorage {

    private static final long EMPTY_QUEUE_TOLERANCE_SECONDS = 5;

    private AbstractFTPStorage executingStorage;

    private boolean opened = true;
    @NotNull
    private BlockingQueue<ITask<?>> ftpTaskQueue = new LinkedBlockingQueue<>();

    public FTPQueueStorage(AbstractFTPStorage executingStorage) {
        this.executingStorage = executingStorage;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && this.opened) {
            try {
                ITask<?> nextFTPTask = this.ftpTaskQueue.poll(EMPTY_QUEUE_TOLERANCE_SECONDS, TimeUnit.SECONDS);

                boolean ftpAvailable = this.executingStorage.isAvailable();

                if (nextFTPTask == null) {
                    if (ftpAvailable) {
                        this.executingStorage.close();
                    }
                } else {
                    if (!ftpAvailable && !this.executingStorage.connect()) {
                        nextFTPTask.cancel(true);
                    }

                    nextFTPTask.call();
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        }

    }

    @Override
    @Deprecated
    public boolean deploy(@NotNull byte[] zipInput, @NotNull ServiceTemplate target) {
        ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.deploy(zipInput, target));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean deploy(@NotNull File directory, @NotNull ServiceTemplate target, @Nullable Predicate<File> fileFilter) {
        ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.deploy(directory, target, fileFilter));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean deploy(@NotNull ZipInputStream inputStream, @NotNull ServiceTemplate serviceTemplate) {
        ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.deploy(inputStream, serviceTemplate));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean deploy(@NotNull Path[] paths, @NotNull ServiceTemplate target) {
        ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.deploy(paths, target));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean deploy(@NotNull File[] files, @NotNull ServiceTemplate target) {
        ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.deploy(files, target));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull File directory) {
        ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.copy(template, directory));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory) {
        ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.copy(template, directory));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull File[] directories) {
        ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.copy(template, directories));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean copy(@NotNull ServiceTemplate template, @NotNull Path[] directories) {
        ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.copy(template, directories));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    @Deprecated
    public byte[] toZipByteArray(@NotNull ServiceTemplate template) {
        ITask<byte[]> ftpTask = new FTPTask<>(() -> this.executingStorage.toZipByteArray(template));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(new byte[0]);
    }

    @Override
    public @Nullable ZipInputStream asZipInputStream(@NotNull ServiceTemplate template) {
        ITask<ZipInputStream> ftpTask = new FTPTask<>(() -> this.executingStorage.asZipInputStream(template));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(null);
    }

    @Override
    public boolean delete(@NotNull ServiceTemplate template) {
        ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.delete(template));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean create(@NotNull ServiceTemplate template) {
        ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.create(template));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean has(@NotNull ServiceTemplate template) {
        ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.has(template));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Nullable
    @Override
    public OutputStream appendOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.createDataTransfer(() -> this.executingStorage.appendOutputStream(template, path));
    }

    @Nullable
    @Override
    public OutputStream newOutputStream(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        return this.createDataTransfer(() -> this.executingStorage.newOutputStream(template, path));
    }

    private OutputStream createDataTransfer(Callable<OutputStream> outputStreamCallable) throws IOException {
        AtomicReference<OutputStream> outputStreamReference = new AtomicReference<>();
        ListenableTask<OutputStream> valueTask = new ListenableTask<>(outputStreamReference::get);

        FTPTask<Void> ftpTask = new FTPTask<>(() -> {
            OutputStreamCloseTask outputStreamCloseTask = new OutputStreamCloseTask(outputStreamCallable.call());

            outputStreamReference.set(outputStreamCloseTask);
            valueTask.call();

            outputStreamCloseTask.get();

            this.executingStorage.completeDataTransfer();

            return null;
        }, valueTask::call);
        this.ftpTaskQueue.add(ftpTask);

        try {
            OutputStream outputStream = valueTask.get();

            if (ftpTask.getException() instanceof IOException) {
                throw (IOException) ftpTask.getException();
            }

            return outputStream;
        } catch (InterruptedException exception) {
            return null;
        }
    }

    @Override
    public boolean createFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        FTPTask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.createFile(template, path));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getOptionalValue(false).orElseThrow(() -> (IOException) ftpTask.getException());
    }

    @Override
    public boolean createDirectory(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        FTPTask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.createDirectory(template, path));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getOptionalValue(false).orElseThrow(() -> (IOException) ftpTask.getException());
    }

    @Override
    public boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        FTPTask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.hasFile(template, path));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getOptionalValue(false).orElseThrow(() -> (IOException) ftpTask.getException());
    }

    @Override
    public boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
        FTPTask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.deleteFile(template, path));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getOptionalValue(false).orElseThrow(() -> (IOException) ftpTask.getException());
    }

    @Override
    public String[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir) throws IOException {
        FTPTask<String[]> ftpTask = new FTPTask<>(() -> this.executingStorage.listFiles(template, dir));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getOptionalValue(new String[0]).orElseThrow(() -> (IOException) ftpTask.getException());
    }

    @Override
    public Collection<ServiceTemplate> getTemplates() {
        ITask<Collection<ServiceTemplate>> ftpTask = new FTPTask<>(() -> this.executingStorage.getTemplates());
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(Collections.emptyList());
    }

    public AbstractFTPStorage getExecutingStorage() {
        return executingStorage;
    }

    public boolean isOpened() {
        return opened;
    }

    @Override
    public String getName() {
        return this.executingStorage.getName();
    }

    @Override
    public void close() throws IOException {
        this.opened = false;
        this.executingStorage.close();
    }

}
