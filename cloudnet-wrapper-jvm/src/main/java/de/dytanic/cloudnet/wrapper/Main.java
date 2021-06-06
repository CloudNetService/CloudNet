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

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.AbstractLogHandler;
import de.dytanic.cloudnet.common.logging.DefaultAsyncLogger;
import de.dytanic.cloudnet.common.logging.DefaultFileLogHandler;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.common.logging.LogOutputStream;
import de.dytanic.cloudnet.wrapper.log.InternalPrintStreamLogHandler;
import de.dytanic.cloudnet.wrapper.log.WrapperLogFormatter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public final class Main {

  private Main() {
    throw new UnsupportedOperationException();
  }

  public static synchronized void main(String... args) throws Throwable {
    LanguageManager.setLanguage(System.getProperty("cloudnet.wrapper.messages.language", "english"));
    LanguageManager
      .addLanguageFile("german", Main.class.getClassLoader().getResourceAsStream("lang/german.properties"));
    LanguageManager
      .addLanguageFile("english", Main.class.getClassLoader().getResourceAsStream("lang/english.properties"));
    LanguageManager
      .addLanguageFile("french", Main.class.getClassLoader().getResourceAsStream("lang/french.properties"));

    ILogger logger = new DefaultAsyncLogger();
    initLogger(logger);

    logger.setLevel(LogLevel.FATAL);

    Wrapper wrapper = new Wrapper(new ArrayList<>(Arrays.asList(args)), logger);
    wrapper.start();
  }

  private static void initLogger(ILogger logger) throws Throwable {
    for (AbstractLogHandler logHandler : new AbstractLogHandler[]{
      new DefaultFileLogHandler(Paths.get(".wrapper", "logs"), "wrapper.%d.log", DefaultFileLogHandler.SIZE_8MB),
      new InternalPrintStreamLogHandler(System.out, System.err)}) {
      logger.addLogHandler(logHandler.setFormatter(new WrapperLogFormatter()));
    }

    System.setOut(new PrintStream(new LogOutputStream(logger, LogLevel.INFO), true, StandardCharsets.UTF_8.name()));
    System.setErr(new PrintStream(new LogOutputStream(logger, LogLevel.ERROR), true, StandardCharsets.UTF_8.name()));
  }
}
