package de.dytanic.cloudnet.common.concurrent;

public class NullCompletableTask<V> extends CompletableTask<V> {
    @Override
    public V call() {
        super.complete(null);
        return null;
    }
}
