package de.dytanic.cloudnet.ext.storage.ftp.storage.queue;

import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import java.util.Optional;
import java.util.concurrent.Callable;

class FTPTask<V> extends ListenableTask<V> {

  private Exception exception;

  FTPTask(Callable<V> callable) {
    super(callable);

    super.onFailure(throwable -> this.exception = (Exception) throwable);
  }

  FTPTask(Callable<V> callable, Runnable finishedRunnable) {
    super(callable);

    super.onFailure(throwable -> this.exception = (Exception) throwable);
    super.onComplete(ignored -> finishedRunnable.run());
    super.onCancelled(ignored -> finishedRunnable.run());
  }

  @Override
  public V getDef(V def) {
    try {
      return super.get();
    } catch (Exception exception) {
      return def;
    }
  }

  Optional<V> getOptionalValue(V def) {
    try {
      return Optional.ofNullable(super.get());
    } catch (InterruptedException ignored) {
      return Optional.ofNullable(def);
    }
  }

  Exception getException() {
    return this.exception;
  }

}
