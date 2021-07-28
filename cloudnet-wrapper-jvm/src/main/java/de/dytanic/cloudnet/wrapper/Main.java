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
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.common.log.LoggingUtils;
import de.dytanic.cloudnet.common.log.defaults.DefaultFileHandler;
import de.dytanic.cloudnet.common.log.defaults.DefaultLogFormatter;
import de.dytanic.cloudnet.common.log.defaults.ThreadedLogRecordDispatcher;
import de.dytanic.cloudnet.common.log.io.LogOutputStream;
import de.dytanic.cloudnet.wrapper.log.InternalPrintStreamLogHandler;
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
    LanguageManager
      .addLanguageFile("chinese", Main.class.getClassLoader().getResourceAsStream("lang/chinese.properties"));

    Logger logger = LogManager.getRootLogger();
    initLogger(logger);

    Wrapper wrapper = new Wrapper(new ArrayList<>(Arrays.asList(args)));
    wrapper.start();
  }

  private static void initLogger(Logger logger) {
    logger.setLevel(LoggingUtils.getDefaultLogLevel());
    logger.setLogRecordDispatcher(ThreadedLogRecordDispatcher.forLogger(logger));

    logger.addHandler(DefaultFileHandler.newInstance(".wrapper/logs/wrapper.%g.log", false)
      .withFormatter(DefaultLogFormatter.INSTANCE));
    logger.addHandler(InternalPrintStreamLogHandler.forSystemStreams().withFormatter(DefaultLogFormatter.INSTANCE));

    System.setErr(LogOutputStream.forSevere(logger).toPrintStream());
    System.setOut(LogOutputStream.forInformative(logger).toPrintStream());
  }
}
