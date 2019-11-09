package de.dytanic.cloudnet.ext.storage.ftp.storage.queue;

import de.dytanic.cloudnet.common.concurrent.ListenableTask;

import java.util.Optional;
import java.util.concurrent.Callable;

class FTPTask<V, T extends Throwable> extends ListenableTask<V> {

    private T throwable;

    FTPTask(Callable<V> callable) {
        super(callable);

        super.onFailure(throwable -> this.throwable = (T) throwable);
    }

    FTPTask(Callable<V> callable, Runnable completeRunnable) {
        super(callable);

        super.onFailure(throwable -> this.throwable = (T) throwable);
        super.onComplete(ignored -> completeRunnable.run());
    }

    T getThrowable() {
        return throwable;
    }

    Optional<V> getOptionalValue(V def) {
        try {
            return Optional.ofNullable(super.get());
        } catch (InterruptedException ignored) {
            return Optional.ofNullable(def);
        }
    }

}
