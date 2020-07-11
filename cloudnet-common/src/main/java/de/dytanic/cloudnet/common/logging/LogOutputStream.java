package de.dytanic.cloudnet.common.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Defines a new ByteArrayOutputStream that convert the bytes into a message and
 * invokes the in constructor exist logger
 */
public class LogOutputStream extends ByteArrayOutputStream {

    /**
     * The logger for this outputStream
     */
    protected final ILogger logger;

    /**
     * The LogLevel in that the logger should log the incoming message
     */
    protected final LogLevel logLevel;

    public LogOutputStream(ILogger logger, LogLevel logLevel) {
        this.logger = logger;
        this.logLevel = logLevel;
    }

    public LogOutputStream(int size, ILogger logger, LogLevel logLevel) {
        super(size);
        this.logger = logger;
        this.logLevel = logLevel;
    }

    public ILogger getLogger() {
        return this.logger;
    }

    public LogLevel getLogLevel() {
        return this.logLevel;
    }

    @Override
    public void flush() throws IOException {
        String input = this.toString(StandardCharsets.UTF_8.name());
        this.reset();

        if (input != null && !input.isEmpty() && !input.equals(System.lineSeparator())) {
            this.logger.log(this.logLevel, input);
        }
    }
}