package de.dytanic.cloudnet.common.concurrent.scheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Test;

public class TaskSchedulerTest implements Callable<String> {

  @Test
  public void testTaskScheduler() throws Exception {

    TaskScheduler taskScheduler = new TaskScheduler(4, null, 1L, false, 15000);

    Assert.assertEquals(4, taskScheduler.getMaxThreads());
    Assert.assertEquals(6,
      taskScheduler.chargeThreadLimit((short) 2).getMaxThreads());

    Future<String> x = taskScheduler.schedule(this);

    Assert.assertEquals(1, taskScheduler.getCurrentThreadSize());

    Future<String> y = taskScheduler.schedule(this);

    Assert.assertEquals(2, taskScheduler.getCurrentThreadSize());

    String result = x.get();
    Assert.assertEquals("Hello World", result);

    y.get();

    taskScheduler.shutdown();
  }

  @Override
  public String call() throws Exception {
    for (int i = 0; i++ < 5; Thread.sleep(2)) {
      ;
    }

    return "Hello World";
  }
}