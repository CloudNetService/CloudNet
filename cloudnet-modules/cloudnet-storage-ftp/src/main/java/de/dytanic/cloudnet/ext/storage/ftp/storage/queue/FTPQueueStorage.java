package de.dytanic.cloudnet.ext.storage.ftp.storage.queue;

import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.storage.ftp.storage.GeneralFTPStorage;
import de.dytanic.cloudnet.template.ITemplateStorage;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public class FTPQueueStorage implements Runnable, ITemplateStorage {

    private GeneralFTPStorage executingStorage;

    private boolean opened = true;
    private Queue<ITask> ftpTaskQueue = new ConcurrentLinkedQueue<>();

    public FTPQueueStorage(GeneralFTPStorage executingStorage) {
        this.executingStorage = executingStorage;
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted() && this.opened) {

            ITask nextFTPTask = this.ftpTaskQueue.poll();

            try {
                if (nextFTPTask == null) {
                    if (this.executingStorage.isAvailable()) {
                        this.executingStorage.close();
                    }
                } else {
                    if (!this.executingStorage.isAvailable() && !this.executingStorage.connect()) {
                        nextFTPTask.cancel(true);
                    }

                    nextFTPTask.call();

                    nextFTPTask.get();
                    Thread.sleep(10);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        }

    }

    @Override
    public boolean deploy(byte[] zipInput, ServiceTemplate target) {
        ITask<Boolean> ftpTask = new ListenableTask<>(() -> this.executingStorage.deploy(zipInput, target));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean deploy(File directory, ServiceTemplate target, Predicate<File> fileFilter) {
        ITask<Boolean> ftpTask = new ListenableTask<>(() -> this.executingStorage.deploy(directory, target, fileFilter));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean deploy(Path[] paths, ServiceTemplate target) {
        ITask<Boolean> ftpTask = new ListenableTask<>(() -> this.executingStorage.deploy(paths, target));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean deploy(File[] files, ServiceTemplate target) {
        ITask<Boolean> ftpTask = new ListenableTask<>(() -> this.executingStorage.deploy(files, target));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean copy(ServiceTemplate template, File directory) {
        ITask<Boolean> ftpTask = new ListenableTask<>(() -> this.executingStorage.copy(template, directory));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean copy(ServiceTemplate template, Path directory) {
        ITask<Boolean> ftpTask = new ListenableTask<>(() -> this.executingStorage.copy(template, directory));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean copy(ServiceTemplate template, File[] directories) {
        ITask<Boolean> ftpTask = new ListenableTask<>(() -> this.executingStorage.copy(template, directories));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean copy(ServiceTemplate template, Path[] directories) {
        ITask<Boolean> ftpTask = new ListenableTask<>(() -> this.executingStorage.copy(template, directories));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public byte[] toZipByteArray(ServiceTemplate template) {
        ITask<byte[]> ftpTask = new ListenableTask<>(() -> this.executingStorage.toZipByteArray(template));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(new byte[0]);
    }

    @Override
    public boolean delete(ServiceTemplate template) {
        ITask<Boolean> ftpTask = new ListenableTask<>(() -> this.executingStorage.delete(template));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean create(ServiceTemplate template) {
        ITask<Boolean> ftpTask = new ListenableTask<>(() -> this.executingStorage.create(template));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public boolean has(ServiceTemplate template) {
        ITask<Boolean> ftpTask = new ListenableTask<>(() -> this.executingStorage.has(template));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getDef(false);
    }

    @Override
    public OutputStream appendOutputStream(ServiceTemplate template, String path) throws IOException {
        return this.createDataTransfer(() -> this.executingStorage.appendOutputStream(template, path));
    }

    @Override
    public OutputStream newOutputStream(ServiceTemplate template, String path) throws IOException {
        return this.createDataTransfer(() -> this.executingStorage.newOutputStream(template, path));
    }

    private OutputStream createDataTransfer(Callable<OutputStream> outputStreamCallable) throws IOException {
        Value<OutputStream> outputStreamValue = new Value<>();
        ListenableTask<OutputStream> valueTask = new ListenableTask<>(outputStreamValue::getValue);

        FTPTask<Void> ftpTask = new FTPTask<>(() -> {
            OutputStreamCloseTask outputStreamCloseTask = new OutputStreamCloseTask(outputStreamCallable.call());

            outputStreamValue.setValue(outputStreamCloseTask);
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
    public boolean createFile(ServiceTemplate template, String path) throws IOException {
        FTPTask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.createFile(template, path));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getOptionalValue(false).orElseThrow(() -> (IOException) ftpTask.getException());
    }

    @Override
    public boolean createDirectory(ServiceTemplate template, String path) throws IOException {
        FTPTask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.createDirectory(template, path));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getOptionalValue(false).orElseThrow(() -> (IOException) ftpTask.getException());
    }

    @Override
    public boolean hasFile(ServiceTemplate template, String path) throws IOException {
        FTPTask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.hasFile(template, path));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getOptionalValue(false).orElseThrow(() -> (IOException) ftpTask.getException());
    }

    @Override
    public boolean deleteFile(ServiceTemplate template, String path) throws IOException {
        FTPTask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.deleteFile(template, path));
        this.ftpTaskQueue.add(ftpTask);

        return ftpTask.getOptionalValue(false).orElseThrow(() -> (IOException) ftpTask.getException());
    }

    @Override
    public String[] listFiles(ServiceTemplate template, String dir) throws IOException {
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
