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

package eu.cloudnetservice.wrapper;

import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.common.log.LoggingUtil;
import eu.cloudnetservice.common.log.defaults.DefaultFileHandler;
import eu.cloudnetservice.common.log.defaults.DefaultLogFormatter;
import eu.cloudnetservice.common.log.defaults.ThreadedLogRecordDispatcher;
import eu.cloudnetservice.wrapper.log.InternalPrintStreamLogHandler;
import java.nio.file.Path;
import java.time.Instant;
import lombok.NonNull;

public final class Main {

  private Main() {
    throw new UnsupportedOperationException();
  }

  public static void main(String... args) throws Throwable {
    var startInstant = Instant.now();
    // language init
    I18n.loadFromLangPath(Main.class);
    I18n.language(System.getProperty("cloudnet.wrapper.messages.language", "en_US"));
    // logger init
    initLogger(LogManager.rootLogger());
    // boot the wrapper
    var wrapper = new Wrapper(args);
    wrapper.start(startInstant);
  }

  private static void initLogger(@NonNull Logger logger) {
    LoggingUtil.removeHandlers(logger);
    var logFilePattern = Path.of(".wrapper", "logs", "wrapper.%g.log");

    logger.setLevel(LoggingUtil.defaultLogLevel());
    logger.logRecordDispatcher(ThreadedLogRecordDispatcher.forLogger(logger));

    logger.addHandler(InternalPrintStreamLogHandler.forSystemStreams().withFormatter(DefaultLogFormatter.END_CLEAN));
    logger.addHandler(
      DefaultFileHandler.newInstance(logFilePattern, false).withFormatter(DefaultLogFormatter.END_LINE_SEPARATOR));
  }
}
