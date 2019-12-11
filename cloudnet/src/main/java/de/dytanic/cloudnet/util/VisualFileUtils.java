package de.dytanic.cloudnet.util;

import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.animation.progressbar.ProgressBarInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

public class VisualFileUtils {

    private VisualFileUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Copies the given InputStream into the given OutputStream and shows a progress bar in the given console IF:
     * - there is no animation running in this console
     * - the given length is >= 0 or {@link InputStream#available()} returns a value >= 0
     *
     * @param console      the console to show the animation in
     * @param inputStream  the source stream
     * @param outputStream the target stream
     * @param fullLength   the full length of the source stream or -1  if not available
     * @return {@code true} if the animation was shown or {@code false} otherwise
     * @throws IOException if an I/O error occurred when copying the source stream into the target stream
     */
    public static boolean copyWithProgressBar(IConsole console, InputStream inputStream, OutputStream outputStream, long fullLength) throws IOException {
        boolean showAnimation = true;

        if (console.isAnimationRunning()) {
            showAnimation = false;
        }

        if (fullLength < 0) {
            fullLength = inputStream.available();
        }

        if (fullLength < 0) {
            showAnimation = false;
        }

        byte[] buffer = new byte[1024];
        int len;

        if (showAnimation) {
            inputStream = new ProgressBarInputStream(console, inputStream, fullLength);
        }

        while ((len = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, len);
        }

        if (inputStream instanceof ProgressBarInputStream) {
            inputStream.close();
        }

        return showAnimation;
    }

    public static void downloadFile(IConsole console, URL url, OutputStream outputStream) throws IOException {
        URLConnection connection = url.openConnection();

        downloadFile(console, connection, outputStream);

        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).disconnect();
        }
    }

    public static void downloadFile(IConsole console, URL url, Path targetFile) throws IOException {
        URLConnection connection = url.openConnection();

        downloadFile(console, connection, targetFile);

        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).disconnect();
        }
    }

    public static void downloadFile(IConsole console, URLConnection connection, OutputStream outputStream) throws IOException {
        connection.connect();

        try (InputStream inputStream = connection.getInputStream()) {
            copyWithProgressBar(console, inputStream, outputStream, getContentLength(connection));
        }
    }

    public static void downloadFile(IConsole console, URLConnection connection, Path targetFile) throws IOException {
        connection.connect();

        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = Files.newOutputStream(targetFile)) {
            copyWithProgressBar(console, inputStream, outputStream, getContentLength(connection));
        }
    }

    public static long getContentLength(URLConnection connection) {
        String headerField = connection.getHeaderField("Content-Length");

        if (headerField != null) {
            try {
                return Long.parseLong(headerField);
            } catch (NumberFormatException ignored) {
            }
        }

        return -1;
    }

}
