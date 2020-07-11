package de.dytanic.cloudnet.console.animation.progressbar;

import de.dytanic.cloudnet.console.IConsole;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ProgressBarInputStream extends InputStream {

    private final ConsoleProgressBarAnimation progressBarAnimation;
    private final InputStream wrapped;

    public ProgressBarInputStream(IConsole console, InputStream wrapped, long length) {
        this(
                new ConsoleDownloadProgressBarAnimation(length, 0, '|', '|', '─', "&e%percent% % ", "| %value%/%length% MB (%byps% KB/s) | %time%"),
                wrapped
        );
        console.startAnimation(this.progressBarAnimation);
    }

    public ProgressBarInputStream(ConsoleProgressBarAnimation progressBarAnimation, InputStream wrapped) {
        this.progressBarAnimation = progressBarAnimation;
        this.wrapped = wrapped;
    }

    public static InputStream wrapDownload(IConsole console, URL url) throws IOException {
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        urlConnection.connect();

        InputStream inputStream = urlConnection.getInputStream();

        long contentLength = urlConnection.getHeaderFieldLong("Content-Length", inputStream.available());
        return console.isAnimationRunning() ? inputStream : new ProgressBarInputStream(console, inputStream, contentLength);
    }


    @Override
    public int read() throws IOException {
        int read = this.wrapped.read();
        this.progressBarAnimation.setCurrentValue(this.progressBarAnimation.getCurrentValue() + 1);
        return read;
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        int read = this.wrapped.read(b);
        this.progressBarAnimation.setCurrentValue(this.progressBarAnimation.getCurrentValue() + read);
        return read;
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        int read = this.wrapped.read(b, off, len);
        this.progressBarAnimation.setCurrentValue(this.progressBarAnimation.getCurrentValue() + read);
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long length = this.wrapped.skip(n);
        this.progressBarAnimation.setCurrentValue(this.progressBarAnimation.getCurrentValue() + length);
        return length;
    }

    public InputStream getWrapped() {
        return this.wrapped;
    }

    public ConsoleProgressBarAnimation getProgressBarAnimation() {
        return this.progressBarAnimation;
    }

    @Override
    public void close() throws IOException {
        this.progressBarAnimation.finish();
        this.wrapped.close();
    }

}
