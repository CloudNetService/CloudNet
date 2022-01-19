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

package eu.cloudnetservice.cloudnet.node;

import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.common.log.LoggingUtil;
import eu.cloudnetservice.cloudnet.common.log.defaults.AcceptingLogHandler;
import eu.cloudnetservice.cloudnet.common.log.defaults.DefaultFileHandler;
import eu.cloudnetservice.cloudnet.common.log.defaults.DefaultLogFormatter;
import eu.cloudnetservice.cloudnet.common.log.defaults.ThreadedLogRecordDispatcher;
import eu.cloudnetservice.cloudnet.common.log.io.LogOutputStream;
import eu.cloudnetservice.cloudnet.node.console.Console;
import eu.cloudnetservice.cloudnet.node.console.JLine3Console;
import eu.cloudnetservice.cloudnet.node.console.log.ColouredLogFormatter;
import java.nio.file.Path;
import lombok.NonNull;

public final class BootLogic {

  private BootLogic() {
    throw new UnsupportedOperationException();
  }

  public static void main(String[] args) throws Throwable {
    // language management init
    I18n.loadFromLangPath(BootLogic.class);
    I18n.language(System.getProperty("cloudnet.messages.language", "en_US"));

    // init logger and console
    Console console = new JLine3Console();
    initLoggerAndConsole(console, LogManager.rootLogger());

    // boot CloudNet
    var nodeInstance = new CloudNet(args, console, LogManager.rootLogger());
    nodeInstance.start();
  }

  private static void initLoggerAndConsole(@NonNull Console console, @NonNull Logger logger) {
    var logFilePattern = Path.of("local", "logs", "cloudnet.%g.log");
    var consoleFormatter = console.hasColorSupport() ? new ColouredLogFormatter() : DefaultLogFormatter.END_CLEAN;

    LoggingUtil.removeHandlers(logger);

    logger.setLevel(LoggingUtil.defaultLogLevel());
    logger.logRecordDispatcher(ThreadedLogRecordDispatcher.forLogger(logger));

    logger.addHandler(AcceptingLogHandler.newInstance(console::writeLine).withFormatter(consoleFormatter));
    logger.addHandler(
      DefaultFileHandler.newInstance(logFilePattern, true).withFormatter(DefaultLogFormatter.END_LINE_SEPARATOR));

    System.setErr(LogOutputStream.forSevere(logger).toPrintStream());
    System.setOut(LogOutputStream.forInformative(logger).toPrintStream());
  }
}
