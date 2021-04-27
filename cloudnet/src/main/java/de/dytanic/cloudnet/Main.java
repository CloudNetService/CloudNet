package de.dytanic.cloudnet;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.AbstractLogHandler;
import de.dytanic.cloudnet.common.logging.DefaultAsyncLogger;
import de.dytanic.cloudnet.common.logging.DefaultFileLogHandler;
import de.dytanic.cloudnet.common.logging.DefaultLogFormatter;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.common.logging.LogOutputStream;
import de.dytanic.cloudnet.console.ConsoleLogHandler;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.JLine3Console;
import de.dytanic.cloudnet.console.log.ColouredLogFormatter;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;

public final class Main {

    private Main() {
        throw new UnsupportedOperationException();
    }

    public static synchronized void main(String... args) throws Throwable {
        LanguageManager.setLanguage(System.getProperty("cloudnet.messages.language", "english"));
        LanguageManager.addLanguageFile("german", Main.class.getClassLoader().getResourceAsStream("lang/german.properties"));
        LanguageManager.addLanguageFile("english", Main.class.getClassLoader().getResourceAsStream("lang/english.properties"));
        LanguageManager.addLanguageFile("french", Main.class.getClassLoader().getResourceAsStream("lang/french.properties"));

        IConsole console = new JLine3Console();
        ILogger logger = new DefaultAsyncLogger();

        logger.setLevel(LogLevel.FATAL);

        initLoggerAndConsole(console, logger);

        CloudNet cloudNet = new CloudNet(Arrays.asList(args), logger, console);
        cloudNet.start();
    }

    private static void initLoggerAndConsole(IConsole console, ILogger logger) throws Throwable {
        for (AbstractLogHandler logHandler : new AbstractLogHandler[]{
                new DefaultFileLogHandler(Paths.get("local", "logs"), "cloudnet.%d.log", DefaultFileLogHandler.SIZE_8MB).setEnableErrorLog(true),
                new ConsoleLogHandler(console).setFormatter(console.hasColorSupport() ? new ColouredLogFormatter() : new DefaultLogFormatter())
        }) {
            logger.addLogHandler(logHandler);
        }

        System.setOut(new PrintStream(new LogOutputStream(logger, LogLevel.INFO), true, StandardCharsets.UTF_8.name()));
        System.setErr(new PrintStream(new LogOutputStream(logger, LogLevel.ERROR), true, StandardCharsets.UTF_8.name()));
    }
}
