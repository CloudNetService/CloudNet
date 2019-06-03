package de.dytanic.cloudnet.common.logging;

import lombok.Getter;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * A standard file logger for this LoggingAPI. All important configurations can be made in the constructor
 */
@Getter
public final class DefaultFileLogHandler extends AbstractLogHandler {

    public static final long SIZE_8MB = 8000000L;

    /*= -------------------------------- =*/

    private final File directory;

    private final String pattern;

    private final long maxBytes;

    /*= -------------------------------- =*/

    private File entry;

    private PrintWriter printWriter;

    private long writternBytes = 0L;

    private int index = 0;

    /**
     * The default constructor with all important configuration
     *
     * @param directory the default storage for the log files
     * @param pattern   the file pattern, for the log files like "app.log" will be to than "app.log.0"
     * @param maxBytes  the maximum bytes, that a log file should have, to switch to the next log file
     */
    public DefaultFileLogHandler(File directory, String pattern, long maxBytes) {
        if (directory == null)
            directory = new File(System.getProperty("cloudnet.logging.fallback.log.directory", "logs"));

        this.directory = directory;
        this.directory.mkdirs();

        this.pattern = pattern;
        this.maxBytes = maxBytes;

        selectLogFile();
    }

    @Override
    public void handle(LogEntry logEntry) {
        if (getFormatter() == null) setFormatter(new DefaultLogFormatter());
        if (entry == null) selectLogFile();
        if (entry.length() > maxBytes) selectLogFile();

        String formatted = getFormatter().format(logEntry);
        this.writternBytes = writternBytes + formatted.getBytes(StandardCharsets.UTF_8).length;

        if (this.writternBytes > maxBytes) selectLogFile();

        printWriter.write(formatted);
        printWriter.flush();
    }

    @Override
    public void close() throws Exception {

        index = 0;
        printWriter.close();
    }

    private void selectLogFile() {
        if (printWriter != null) printWriter.close();
        if (writternBytes != 0L) writternBytes = 0L;

        entry = null;
        File file;

        while (entry == null) {
            file = new File(directory, pattern + "." + index);

            try {

                if (!file.exists()) file.createNewFile();

                if (file.length() < maxBytes) {
                    this.entry = file;
                    this.printWriter = new PrintWriter(new FileWriter(this.entry, true));
                    break;
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

            index++;
        }

        index = 0;
    }
}