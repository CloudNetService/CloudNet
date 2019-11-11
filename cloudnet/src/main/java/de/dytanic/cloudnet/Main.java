package de.dytanic.cloudnet;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.*;
import de.dytanic.cloudnet.console.ConsoleLogHandler;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.JLine2Console;
import de.dytanic.cloudnet.console.log.ColouredLogFormatter;
import de.dytanic.cloudnet.console.util.HeaderReader;
import de.dytanic.cloudnet.util.CreditsUtil;

import java.io.File;
import java.util.Arrays;

public final class Main {

    private Main() {
        throw new UnsupportedOperationException();
    }

    public static synchronized void main(String... args) throws Throwable {
        LanguageManager.setLanguage(System.getProperty("cloudnet.messages.language", "english"));
        LanguageManager.addLanguageFile("german", Main.class.getClassLoader().getResourceAsStream("lang/german.properties"));
        LanguageManager.addLanguageFile("english", Main.class.getClassLoader().getResourceAsStream("lang/english.properties"));

        IConsole console = new JLine2Console();
        ILogger logger = new DefaultAsyncLogger();

        logger.setLevel(LogLevel.ALL);

        initLoggerAndConsole(console, logger);
        HeaderReader.readAndPrintHeader(console);
        CreditsUtil.printContributorNames(null, logger);

        CloudNet cloudNet = new CloudNet(Arrays.asList(args), logger, console);
        cloudNet.start();
    }

    private static void initLoggerAndConsole(IConsole console, ILogger logger) throws Throwable {
        for (AbstractLogHandler logHandler : new AbstractLogHandler[]{
                new DefaultFileLogHandler(new File("local/logs"), "cloudnet.log", DefaultFileLogHandler.SIZE_8MB).setEnableErrorLog(true),
                new ConsoleLogHandler(console).setFormatter(console.hasColorSupport() ? new ColouredLogFormatter() : new DefaultLogFormatter())
        }) {
            logger.addLogHandler(logHandler);
        }

        System.setOut(new AsyncPrintStream(new LogOutputStream(logger, LogLevel.INFO)));
        System.setErr(new AsyncPrintStream(new LogOutputStream(logger, LogLevel.ERROR)));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.close();
                console.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }));
    }
}
