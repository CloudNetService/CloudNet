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

package eu.cloudnetservice.node;

import dev.derklaro.aerogel.Bindings;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.auto.AutoAnnotationRegistry;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.common.log.LoggingUtil;
import eu.cloudnetservice.common.log.defaults.AcceptingLogHandler;
import eu.cloudnetservice.common.log.defaults.DefaultFileHandler;
import eu.cloudnetservice.common.log.defaults.DefaultLogFormatter;
import eu.cloudnetservice.common.log.defaults.ThreadedLogRecordDispatcher;
import eu.cloudnetservice.common.log.io.LogOutputStream;
import eu.cloudnetservice.node.console.Console;
import eu.cloudnetservice.node.console.log.ColoredLogFormatter;
import java.nio.file.Path;
import java.time.Instant;
import lombok.NonNull;

public final class BootLogic {

  private BootLogic() {
    throw new UnsupportedOperationException();
  }

  public static void main(String[] args) throws Throwable {
    var startInstant = Instant.now();

    // initialize injectors
    var injector = Injector.newInjector();
    installBindings(injector);

    // initial bindings
    injector.install(Bindings.fixed(Element.forType(String[].class).requireName("consoleArgs"), args));
    injector.install(Bindings.fixed(Element.forType(Instant.class).requireName("startInstant"), Instant.now()));
    injector.install(Bindings.fixed(Element.forType(Logger.class).requireName("root"), LogManager.rootLogger()));

    // init logger and console
    var console = injector.instance(Console.class);
    initLoggerAndConsole(console, LogManager.rootLogger());

    // boot CloudNet
    var nodeInstance = injector.instance(Node.class);
    nodeInstance.start(startInstant);
  }

  private static void installBindings(@NonNull Injector injector) {
    var annotationRegistry = AutoAnnotationRegistry.newInstance();
    annotationRegistry.installBindings(BootLogic.class.getClassLoader(), "auto-factories.txt", injector);
  }

  private static void initLoggerAndConsole(@NonNull Console console, @NonNull Logger logger) {
    var logFilePattern = Path.of(System.getProperty("cloudnet.log.path", "local/logs"), "cloudnet.%g.log");
    var consoleFormatter = console.hasColorSupport() ? new ColoredLogFormatter() : DefaultLogFormatter.END_CLEAN;

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
