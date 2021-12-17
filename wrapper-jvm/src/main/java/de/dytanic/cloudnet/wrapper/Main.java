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

package de.dytanic.cloudnet.wrapper;

import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.common.log.LoggingUtils;
import de.dytanic.cloudnet.common.log.defaults.DefaultFileHandler;
import de.dytanic.cloudnet.common.log.defaults.DefaultLogFormatter;
import de.dytanic.cloudnet.common.log.defaults.ThreadedLogRecordDispatcher;
import de.dytanic.cloudnet.wrapper.log.InternalPrintStreamLogHandler;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;

public final class Main {

  private Main() {
    throw new UnsupportedOperationException();
  }

  public static synchronized void main(String... args) throws Throwable {
    // language init
    I18n.loadFromLanguageRegistryFile(Main.class.getClassLoader());
    I18n.language(System.getProperty("cloudnet.wrapper.messages.language", "english"));
    // logger init
    initLogger(LogManager.rootLogger());
    // boot the wrapper
    var wrapper = new Wrapper(args);
    wrapper.start();
  }

  private static void initLogger(@NotNull Logger logger) {
    LoggingUtils.removeHandlers(logger);
    var logFilePattern = Paths.get(".wrapper", "logs", "wrapper.%g.log");

    logger.setLevel(LoggingUtils.defaultLogLevel());
    logger.logRecordDispatcher(ThreadedLogRecordDispatcher.forLogger(logger));

    logger.addHandler(InternalPrintStreamLogHandler.forSystemStreams().withFormatter(DefaultLogFormatter.END_CLEAN));
    logger.addHandler(
      DefaultFileHandler.newInstance(logFilePattern, false).withFormatter(DefaultLogFormatter.END_LINE_SEPARATOR));
  }
}
