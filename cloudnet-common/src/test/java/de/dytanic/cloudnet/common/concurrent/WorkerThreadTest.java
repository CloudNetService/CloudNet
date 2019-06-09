package de.dytanic.cloudnet.common.concurrent;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class WorkerThreadTest {

    @Test
    public void testWorker() throws Exception {
        WorkerThread worker = new WorkerThread();
        worker.start();

        AtomicInteger value = new AtomicInteger();
        ITask<Integer> task1 = worker.submit(() -> 1);

        value.set(task1.get());
        Assert.assertEquals(1, value.get());

        ITask<Integer> task2 = worker.submit(() -> 2, new ITaskListener<Integer>() {

            @Override
            public void onComplete(ITask<Integer> task, Integer result) {
                value.set(result);
            }
        });
        task2.get();
        Assert.assertEquals(2, value.get());

        worker.shutdownNow();
    }
}