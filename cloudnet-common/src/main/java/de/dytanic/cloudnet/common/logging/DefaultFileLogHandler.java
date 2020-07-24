package de.dytanic.cloudnet.common.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * A standard file logger for this LoggingAPI. All important configurations can be made in the constructor
 */
public final class DefaultFileLogHandler extends AbstractLogHandler {

    public static final long SIZE_8MB = 8000000L;


    private final File directory;

    private final String pattern;

    private final long maxBytes;


    private File entry;

    private PrintWriter printWriter;

    private long writternBytes = 0L;

    private File errorFile;
    private long writtenErrorBytes = 0L;
    private PrintWriter errorWriter;

    /**
     * The default constructor with all important configuration
     *
     * @param directory the default storage for the log files
     * @param pattern   the file pattern, for the log files like "app.log" will be to than "app.log.0"
     * @param maxBytes  the maximum bytes, that a log file should have, to switch to the next log file
     */
    public DefaultFileLogHandler(File directory, String pattern, long maxBytes) {
        if (directory == null) {
            directory = new File(System.getProperty("cloudnet.logging.fallback.log.directory", "logs"));
        }

        this.directory = directory;
        this.directory.mkdirs();

        this.pattern = pattern;
        this.maxBytes = maxBytes;

        this.entry = this.initPrintWriter(this.selectLogFile(this.printWriter, this.pattern));
    }

    /**
     * Enables/disables the error log file (directory/error.log)
     *
     * @param enableErrorLog if the file should be created and filled with every error in the console
     */
    public DefaultFileLogHandler setEnableErrorLog(boolean enableErrorLog) throws IOException {
        if (enableErrorLog && this.errorWriter == null) {
            this.errorFile = this.initErrorWriter(this.selectLogFile(null, "error.log"));
            this.errorWriter = new PrintWriter(new FileWriter(this.errorFile, true));
        } else if (!enableErrorLog && this.errorWriter != null) {
            this.errorWriter.close();
            this.errorWriter = null;
        }
        return this;
    }

    @Override
    public void handle(LogEntry logEntry) {
        if (this.getFormatter() == null) {
            this.setFormatter(new DefaultLogFormatter());
        }

        if (this.entry == null || this.entry.length() > this.maxBytes) {
            this.entry = this.initPrintWriter(this.selectLogFile(this.printWriter, this.pattern));
        }

        String formatted = this.getFormatter().format(logEntry);
        byte[] formattedBytes = formatted.getBytes(StandardCharsets.UTF_8);
        this.writternBytes = this.writternBytes + formattedBytes.length;

        if (this.writternBytes > this.maxBytes) {
            this.entry = this.initPrintWriter(this.selectLogFile(this.printWriter, this.pattern));
        }

        this.printWriter.write(formatted);
        this.printWriter.flush();

        if (this.errorWriter != null && logEntry.getLogLevel().getLevel() >= 126 && logEntry.getLogLevel().getLevel() <= 127) {
            if (this.errorFile == null || this.errorFile.length() > this.maxBytes) {
                this.errorFile = this.initErrorWriter(this.selectLogFile(this.errorWriter, "error.log"));
            }

            this.writtenErrorBytes += formattedBytes.length;

            if (this.writtenErrorBytes > this.maxBytes) {
                this.errorFile = this.initErrorWriter(this.selectLogFile(this.errorWriter, "error.log"));
            }

            this.errorWriter.write(formatted);
            this.errorWriter.flush();
        }
    }

    @Override
    public void close() {
        this.printWriter.flush();
        this.printWriter.close();
    }

    public File getDirectory() {
        return this.directory;
    }

    public String getPattern() {
        return this.pattern;
    }

    public long getMaxBytes() {
        return this.maxBytes;
    }

    public File getEntry() {
        return this.entry;
    }

    public PrintWriter getPrintWriter() {
        return this.printWriter;
    }

    public long getWritternBytes() {
        return this.writternBytes;
    }

    private File selectLogFile(PrintWriter printWriter, String pattern) {
        if (printWriter != null) {
            printWriter.close();
        }

        this.entry = null;
        File file;

        int index = 0;

        while (true) {
            file = new File(this.directory, pattern + "." + index);

            try {

                if (!file.exists()) {
                    file.createNewFile();
                }

                if (file.length() < this.maxBytes) {
                    index = 0;
                    return file;
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }

            index++;
        }
    }

    private File initPrintWriter(File file) {
        try {
            this.printWriter = new PrintWriter(new FileWriter(file, true));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return file;
    }

    private File initErrorWriter(File file) {
        try {
            this.errorWriter = new PrintWriter(new FileWriter(file, true));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return file;
    }
}
