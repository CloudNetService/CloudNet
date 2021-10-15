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

package de.dytanic.cloudnet;

import de.dytanic.cloudnet.command.CommandProvider;
import de.dytanic.cloudnet.command.defaults.DefaultCommandProvider;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.common.log.LoggingUtils;
import de.dytanic.cloudnet.common.log.defaults.AcceptingLogHandler;
import de.dytanic.cloudnet.common.log.defaults.DefaultFileHandler;
import de.dytanic.cloudnet.common.log.defaults.DefaultLogFormatter;
import de.dytanic.cloudnet.common.log.defaults.ThreadedLogRecordDispatcher;
import de.dytanic.cloudnet.common.log.io.LogOutputStream;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.JLine3Console;
import de.dytanic.cloudnet.console.log.ColouredLogFormatter;
import de.dytanic.cloudnet.log.QueuedConsoleLogHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Formatter;
import org.jetbrains.annotations.NotNull;

public final class BootLogic {

  private BootLogic() {
    throw new UnsupportedOperationException();
  }

  public static synchronized void main(String[] args) throws Throwable {
    // init logger and console
    CommandProvider commandProvider = new DefaultCommandProvider();
    IConsole console = new JLine3Console(commandProvider);
    initLoggerAndConsole(console, LogManager.getRootLogger());

    LanguageManager.setLanguage(System.getProperty("cloudnet.messages.language", "english"));
    LanguageManager
      .addLanguageFile("german", BootLogic.class.getClassLoader().getResourceAsStream("lang/german.properties"));
    LanguageManager
      .addLanguageFile("english", BootLogic.class.getClassLoader().getResourceAsStream("lang/english.properties"));
    LanguageManager
      .addLanguageFile("french", BootLogic.class.getClassLoader().getResourceAsStream("lang/french.properties"));
    LanguageManager
      .addLanguageFile("chinese", BootLogic.class.getClassLoader().getResourceAsStream("lang/chinese.properties"));
    // boot CloudNet
    CloudNet nodeInstance = new CloudNet(args, console, commandProvider);
    nodeInstance.start();



    /*IConsole console = new JLine3Console();
    Logger logger = LogManager.getRootLogger();

    initLoggerAndConsole(console, logger);

    CloudNet cloudNet = new CloudNet(args, logger, console);
    cloudNet.start();

 */
  }

  private static void initLoggerAndConsole(@NotNull IConsole console, @NotNull Logger logger) {
    Path logFilePattern = Paths.get("local", "logs", "cloudnet.%g.log");
    Formatter consoleFormatter = console.hasColorSupport() ? new ColouredLogFormatter() : DefaultLogFormatter.END_CLEAN;

    LoggingUtils.removeHandlers(logger);

    logger.setLevel(LoggingUtils.getDefaultLogLevel());
    logger.setLogRecordDispatcher(ThreadedLogRecordDispatcher.forLogger(logger));

    logger.addHandler(new QueuedConsoleLogHandler());
    logger.addHandler(AcceptingLogHandler.newInstance(console::writeLine).withFormatter(consoleFormatter));
    logger.addHandler(
      DefaultFileHandler.newInstance(logFilePattern, true).withFormatter(DefaultLogFormatter.END_LINE_SEPARATOR));

    System.setErr(LogOutputStream.forSevere(logger).toPrintStream());
    System.setOut(LogOutputStream.forInformative(logger).toPrintStream());
  }
}
