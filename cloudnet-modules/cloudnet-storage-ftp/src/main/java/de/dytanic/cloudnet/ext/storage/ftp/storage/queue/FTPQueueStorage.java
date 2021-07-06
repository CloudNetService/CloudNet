/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.ext.storage.ftp.storage.queue;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableSupplier;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.driver.template.defaults.DefaultAsyncTemplateStorage;
import de.dytanic.cloudnet.ext.storage.ftp.storage.AbstractFTPStorage;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FTPQueueStorage extends DefaultAsyncTemplateStorage implements Runnable, TemplateStorage {

  private static final long EMPTY_QUEUE_TOLERANCE_SECONDS = 5;

  private final AbstractFTPStorage executingStorage;
  @NotNull
  private final BlockingQueue<ITask<?>> ftpTaskQueue = new LinkedBlockingQueue<>();
  private boolean opened = true;

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

  private <V> ITask<V> addTask(FTPTask<V> task) {
    this.ftpTaskQueue.add(task);
    return task;
  }

  @Override
  public void close() throws IOException {
    this.opened = false;
    this.executingStorage.close();
  }

  @Override
  public @NotNull ITask<Boolean> deployAsync(@NotNull Path directory, @NotNull ServiceTemplate target,
    @Nullable Predicate<Path> fileFilter) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.deploy(directory, target, fileFilter)));
  }

  @Override
  public @NotNull ITask<Boolean> deployAsync(@NotNull InputStream inputStream, @NotNull ServiceTemplate target) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.deploy(inputStream, target)));
  }

  @Override
  public @NotNull ITask<Boolean> copyAsync(@NotNull ServiceTemplate template, @NotNull Path directory) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.copy(template, directory)));
  }

  public boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory) {
    ITask<Boolean> ftpTask = new FTPTask<>(() -> this.executingStorage.copy(template, directory));
    this.ftpTaskQueue.add(ftpTask);

    return ftpTask.getDef(false);
  }

  @Override
  public @NotNull ITask<InputStream> zipTemplateAsync(@NotNull ServiceTemplate template) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.zipTemplate(template)));
  }

  @Override
  public @NotNull ITask<Boolean> deleteAsync(@NotNull ServiceTemplate template) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.delete(template)));
  }

  @Override
  public @NotNull ITask<Boolean> createAsync(@NotNull ServiceTemplate template) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.create(template)));
  }

  @Override
  public @NotNull ITask<Boolean> hasAsync(@NotNull ServiceTemplate template) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.has(template)));
  }

  @Override
  public @NotNull ITask<OutputStream> appendOutputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return CompletableTask.supplyAsync(() -> this
      .createDataTransfer(() -> this.executingStorage.appendOutputStream(template, path),
        CloseableTask::toOutputStream));
  }

  @Override
  public @NotNull ITask<OutputStream> newOutputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return CompletableTask.supplyAsync(() -> this
      .createDataTransfer(() -> this.executingStorage.newOutputStream(template, path), CloseableTask::toOutputStream));
  }

  private <C extends Closeable, S> S createDataTransfer(ThrowableSupplier<C, IOException> streamSupplier,
    Function<CloseableTask<C>, S> streamMapper) throws IOException {
    CompletableTask<CloseableTask<C>> task = new CompletableTask<>();

    FTPTask<Void> ftpTask = new FTPTask<>(() -> {
      C c = streamSupplier.get();
      CloseableTask<C> resultStream = new CloseableTask<>(c);

      task.complete(resultStream);

      resultStream.get();

      this.executingStorage.completeDataTransfer();

      return null;
    }, task::call);
    this.ftpTaskQueue.add(ftpTask);

    try {
      CloseableTask<C> closeable = task.get();

      if (ftpTask.getException() instanceof IOException) {
        throw (IOException) ftpTask.getException();
      }

      return streamMapper.apply(closeable);
    } catch (InterruptedException | ExecutionException exception) {
      return null;
    }
  }

  @Override
  public @NotNull ITask<Boolean> createFileAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.createFile(template, path)));
  }

  @Override
  public @NotNull ITask<Boolean> createDirectoryAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.createDirectory(template, path)));
  }

  @Override
  public @NotNull ITask<Boolean> hasFileAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.hasFile(template, path)));
  }

  @Override
  public @NotNull ITask<Boolean> deleteFileAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.deleteFile(template, path)));
  }

  @Override
  public @NotNull ITask<InputStream> newInputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return CompletableTask.supplyAsync(() -> this
      .createDataTransfer(() -> this.executingStorage.newInputStream(template, path), CloseableTask::toInputStream));
  }

  @Override
  public @NotNull ITask<FileInfo> getFileInfoAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.getFileInfo(template, path)));
  }

  @Override
  public @NotNull ITask<FileInfo[]> listFilesAsync(@NotNull ServiceTemplate template, @NotNull String dir,
    boolean deep) {
    return this.addTask(new FTPTask<>(() -> this.executingStorage.listFiles(template, dir, deep)));
  }

  @Override
  public @NotNull ITask<Collection<ServiceTemplate>> getTemplatesAsync() {
    return this.addTask(new FTPTask<>(this.executingStorage::getTemplates));
  }

  @Override
  public @NotNull ITask<Void> closeAsync() {
    try {
      this.close();
      return CompletedTask.create(null);
    } catch (IOException exception) {
      return CompletedTask.createFailed(exception);
    }
  }

  public AbstractFTPStorage getExecutingStorage() {
    return this.executingStorage;
  }

  public boolean isOpened() {
    return this.opened;
  }

  @Override
  public String getName() {
    return this.executingStorage.getName();
  }
}
