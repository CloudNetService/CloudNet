package de.dytanic.cloudnet.wrapper;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.*;
import de.dytanic.cloudnet.wrapper.log.InternalPrintStreamLogHandler;
import de.dytanic.cloudnet.wrapper.log.WrapperLogFormatter;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public final class Main {

    private Main() {
        throw new UnsupportedOperationException();
    }

    public static synchronized void main(String... args) throws Throwable {
        LanguageManager.setLanguage(System.getProperty("cloudnet.wrapper.messages.language", "english"));
        LanguageManager.addLanguageFile("german", Main.class.getClassLoader().getResourceAsStream("lang/german.properties"));
        LanguageManager.addLanguageFile("english", Main.class.getClassLoader().getResourceAsStream("lang/english.properties"));

        ILogger logger = new DefaultAsyncLogger();
        initLogger(logger);

        logger.setLevel(LogLevel.DEBUG);

        Wrapper wrapper = new Wrapper(new ArrayList<>(Arrays.asList(args)), logger);
        wrapper.start();
    }

    private static void initLogger(ILogger logger) throws Throwable {
        for (AbstractLogHandler logHandler : new AbstractLogHandler[]{
                new DefaultFileLogHandler(new File(".wrapper/logs"), "wrapper.log", DefaultFileLogHandler.SIZE_8MB),
                new InternalPrintStreamLogHandler(System.out, System.err)}) {
            logger.addLogHandler(logHandler.setFormatter(new WrapperLogFormatter()));
        }

        System.setOut(new PrintStream(new LogOutputStream(logger, LogLevel.INFO), true, StandardCharsets.UTF_8.name()));
        System.setErr(new PrintStream(new LogOutputStream(logger, LogLevel.ERROR), true, StandardCharsets.UTF_8.name()));
    }
}