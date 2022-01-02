/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.wrapper;

import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.common.log.LoggingUtils;
import eu.cloudnetservice.cloudnet.common.log.defaults.DefaultFileHandler;
import eu.cloudnetservice.cloudnet.common.log.defaults.DefaultLogFormatter;
import eu.cloudnetservice.cloudnet.common.log.defaults.ThreadedLogRecordDispatcher;
import eu.cloudnetservice.cloudnet.wrapper.log.InternalPrintStreamLogHandler;
import java.nio.file.Path;
import lombok.NonNull;

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

  private static void initLogger(@NonNull Logger logger) {
    LoggingUtils.removeHandlers(logger);
    var logFilePattern = Path.of(".wrapper", "logs", "wrapper.%g.log");

    logger.setLevel(LoggingUtils.defaultLogLevel());
    logger.logRecordDispatcher(ThreadedLogRecordDispatcher.forLogger(logger));

    logger.addHandler(InternalPrintStreamLogHandler.forSystemStreams().withFormatter(DefaultLogFormatter.END_CLEAN));
    logger.addHandler(
      DefaultFileHandler.newInstance(logFilePattern, false).withFormatter(DefaultLogFormatter.END_LINE_SEPARATOR));
  }
}
