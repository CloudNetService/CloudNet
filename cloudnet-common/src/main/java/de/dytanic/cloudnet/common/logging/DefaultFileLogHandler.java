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

        this.entry = this.initPrintWriter(selectLogFile(this.printWriter, this.writternBytes, this.pattern));
    }

    /**
     * Enables/disables the error log file (directory/error.log)
     *
     * @param enableErrorLog if the file should be created and filled with every error in the console
     */
    public DefaultFileLogHandler setEnableErrorLog(boolean enableErrorLog) throws IOException {
        if (enableErrorLog && this.errorWriter == null) {
            this.errorFile = this.initErrorWriter(this.selectLogFile(null, this.writtenErrorBytes, "error.log"));
            this.errorWriter = new PrintWriter(new FileWriter(this.errorFile, true));
        } else if (!enableErrorLog && this.errorWriter != null) {
            this.errorWriter.close();
            this.errorWriter = null;
        }
        return this;
    }

    @Override
    public void handle(LogEntry logEntry) {
        if (getFormatter() == null) {
            setFormatter(new DefaultLogFormatter());
        }

        if (entry == null || this.entry.length() > maxBytes) {
            this.entry = this.initPrintWriter(selectLogFile(this.printWriter, this.writternBytes, this.pattern));
        }

        String formatted = getFormatter().format(logEntry);
        byte[] formattedBytes = formatted.getBytes(StandardCharsets.UTF_8);
        this.writternBytes = writternBytes + formattedBytes.length;

        if (this.writternBytes > maxBytes) {
            this.entry = this.initPrintWriter(selectLogFile(this.printWriter, this.writternBytes, this.pattern));
        }

        printWriter.write(formatted);
        printWriter.flush();

        if (this.errorWriter != null && logEntry.getLogLevel().getLevel() >= 126 && logEntry.getLogLevel().getLevel() <= 127) {
            if (this.errorFile == null || this.errorFile.length() > maxBytes) {
                this.errorFile = this.initErrorWriter(selectLogFile(this.errorWriter, this.writtenErrorBytes, "error.log"));
            }

            this.writtenErrorBytes += formattedBytes.length;

            if (this.writtenErrorBytes > maxBytes) {
                this.errorFile = this.initErrorWriter(selectLogFile(this.errorWriter, this.writtenErrorBytes, "error.log"));
            }

            this.errorWriter.write(formatted);
            this.errorWriter.flush();
        }
    }

    @Override
    public void close() {
        printWriter.flush();
        printWriter.close();
    }

    public File getDirectory() {
        return directory;
    }

    public String getPattern() {
        return pattern;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public File getEntry() {
        return entry;
    }

    public PrintWriter getPrintWriter() {
        return printWriter;
    }

    public long getWritternBytes() {
        return writternBytes;
    }

    private File selectLogFile(PrintWriter printWriter, long writternBytes, String pattern) {
        if (printWriter != null) {
            printWriter.close();
        }
        if (writternBytes != 0L) {
        }

        entry = null;
        File file;

        int index = 0;

        while (true) {
            file = new File(directory, pattern + "." + index);

            try {

                if (!file.exists()) {
                    file.createNewFile();
                }

                if (file.length() < maxBytes) {
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
