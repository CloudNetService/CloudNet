package de.dytanic.cloudnet.common.logging;

import org.jetbrains.annotations.NotNull;
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

        logger.log(LogLevel.INFO, DefaultAsyncLoggerTest.class, "My log message!", "foo", "bar");

        Assert.assertNull(this.data);

        Thread.sleep(100);
        Assert.assertTrue(this.data != null && this.data.equals("My log message!"));

        logger.close();

        Assert.assertTrue(this.closed);
    }

    private class LogHandler implements ILogHandler {

        @Override
        public void handle(@NotNull LogEntry logEntry) {
            DefaultAsyncLoggerTest.this.data = logEntry.getMessages()[0];

            Assert.assertNotNull("data transfer", DefaultAsyncLoggerTest.this.data);
        }

        @Override
        public void close() {
            DefaultAsyncLoggerTest.this.closed = true;
        }
    }

}