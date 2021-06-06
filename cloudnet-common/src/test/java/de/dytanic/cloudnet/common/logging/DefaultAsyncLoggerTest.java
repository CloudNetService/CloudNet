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
