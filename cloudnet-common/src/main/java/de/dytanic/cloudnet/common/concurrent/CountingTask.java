package de.dytanic.cloudnet.common.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

public class CountingTask<V> extends CompletableTask<V> {

    private final V value;

    private final AtomicInteger count;

    public CountingTask(V value, int initialValue) {
        this.value = value;
        this.count = new AtomicInteger(initialValue);

        if (initialValue <= 0) {
            this.complete(this.value);
        }
    }

    public void countUp() {
        this.count.getAndIncrement();
    }

    public void countDown() {
        if (this.count.decrementAndGet() <= 0) {
            super.complete(this.value);
        }
    }

    public int countValue() {
        return this.count.get();
    }

}
