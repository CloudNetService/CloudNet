package de.dytanic.cloudnet.common.concurrent;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultTaskSchedulerTest implements Callable<String> {

    @Test
    public void testDefaultTaskScheduler() throws Exception {
        ITaskScheduler scheduler = new DefaultTaskScheduler(4, 50000, 50);

        Assert.assertEquals(4, scheduler.getMaxThreadSize());

        IScheduledTask<String> x = scheduler.schedule(this);

        Assert.assertEquals(1, scheduler.getCurrentWorkerCount());

        AtomicInteger yTaskCount = new AtomicInteger();

        IScheduledTask<String> y = scheduler.schedule(() -> {
            yTaskCount.incrementAndGet();

            return "Hello, world";
        });

        String result = x.get();
        Assert.assertEquals("Hello World", result);

        y.get();
        Assert.assertEquals(1, yTaskCount.get());

        long delayValue = System.currentTimeMillis();
        IScheduledTask<String> delayed = scheduler.schedule(() -> "test_string", 20, TimeUnit.MILLISECONDS);

        delayed.get();
        delayValue = System.currentTimeMillis() - delayValue;

        Assert.assertTrue(String.format("Delay should be at least 20ms, but was %dms!", delayValue), delayValue >= 20);

        delayValue = System.currentTimeMillis();
        delayed = scheduler.schedule(() -> null, 1, TimeUnit.SECONDS);

        delayed.get();
        delayValue = System.currentTimeMillis() - delayValue;

        Assert.assertTrue(String.format("Delay should be at least 1000ms, but was %dms!", delayValue), delayValue >= 1000);

        IScheduledTask<Integer> callbackTask = scheduler.schedule(new CallableCounter(), 0, 1, 5);
        Assert.assertEquals(5, callbackTask.get().intValue());

        scheduler.cancelAll();
    }

    @Override
    public String call() throws Exception {
        for (int i = 0; i++ < 5; Thread.sleep(2)) {
        }

        return "Hello World";
    }

    private final class CallableCounter implements Callable<Integer> {

        private int counter = 0;

        @Override
        public Integer call() throws Exception {
            return ++this.counter;
        }
    }
}