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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Formatter;
import java.util.logging.Level;

public final class Main {

  private Main() {
    throw new UnsupportedOperationException();
  }

  public static synchronized void main(String... args) throws Throwable {
    LanguageManager.setLanguage(System.getProperty("cloudnet.messages.language", "english"));
    LanguageManager
      .addLanguageFile("german", Main.class.getClassLoader().getResourceAsStream("lang/german.properties"));
    LanguageManager
      .addLanguageFile("english", Main.class.getClassLoader().getResourceAsStream("lang/english.properties"));
    LanguageManager
      .addLanguageFile("french", Main.class.getClassLoader().getResourceAsStream("lang/french.properties"));
    LanguageManager
      .addLanguageFile("chinese", Main.class.getClassLoader().getResourceAsStream("lang/chinese.properties"));

    IConsole console = new JLine3Console();
    Logger logger = LogManager.getRootLogger();

    initLoggerAndConsole(console, logger);
    logger.log(Level.SEVERE, "Huhu", new IllegalStateException("hu"));

    CloudNet cloudNet = new CloudNet(args, logger, console);
    cloudNet.start();
  }

  private static void initLoggerAndConsole(IConsole console, Logger logger) {
    Path logFilePattern = Paths.get("local", "logs", "cloudnet.%g.log");
    Formatter consoleFormatter = console.hasColorSupport() ? new ColouredLogFormatter() : DefaultLogFormatter.INSTANCE;

    LoggingUtils.removeHandlers(logger);

    logger.setLevel(LoggingUtils.getDefaultLogLevel());
    logger.setLogRecordDispatcher(ThreadedLogRecordDispatcher.forLogger(logger));

    logger.addHandler(
      DefaultFileHandler.newInstance(logFilePattern, true).withFormatter(DefaultLogFormatter.INSTANCE));
    logger.addHandler(AcceptingLogHandler.newInstance(console::writeLine).withFormatter(consoleFormatter));

    System.setErr(LogOutputStream.forSevere(logger).toPrintStream());
    System.setOut(LogOutputStream.forInformative(logger).toPrintStream());
  }
}
