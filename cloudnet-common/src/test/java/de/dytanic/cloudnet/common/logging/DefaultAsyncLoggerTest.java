package de.dytanic.cloudnet.common.logging;

import org.junit.Assert;
import org.junit.Test;

public class DefaultAsyncLoggerTest {

  private String data;

  private boolean closed = false;

  @Test
  public void testLogging() throws Exception {

    ILogger logger = new DefaultAsyncLogger();

    LogHandler logHandler = new LogHandler();

    Assert.assertNotNull(logger.addLogHandler(logHandler));
    Assert.assertTrue(logger.getLogHandlers().iterator().hasNext());
    Assert.assertTrue(logger.hasAsyncSupport());

    logger.log(LogLevel.INFO, DefaultAsyncLoggerTest.class, "My log message!",
      "foo", "bar");

    Assert.assertNull(data);

    Thread.sleep(5);
    Assert.assertTrue(data != null && data.equals("My log message!"));

    logger.close();

    Assert.assertTrue(closed);
  }

  private class LogHandler implements ILogHandler {

    @Override
    public void handle(LogEntry logEntry) {
      data = logEntry.getMessages()[0];

      Assert.assertNotNull("data transfer", data);
    }

    @Override
    public void close() {
      closed = true;
    }
  }

}