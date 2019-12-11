package de.dytanic.cloudnet.console.animation.progressbar;

import de.dytanic.cloudnet.console.IConsole;

import java.io.IOException;
import java.io.InputStream;

public class ProgressBarInputStream extends InputStream {

    private ConsoleProgressBarAnimation progressBarAnimation;
    private InputStream wrapped;

    public ProgressBarInputStream(ConsoleProgressBarAnimation progressBarAnimation, InputStream wrapped) {
        this.progressBarAnimation = progressBarAnimation;
        this.wrapped = wrapped;
    }

    public ProgressBarInputStream(IConsole console, InputStream wrapped, long length) {
        this(
                new ConsoleDownloadProgressBarAnimation(length, 0, '█', '█', '-', "&e%percent% % ", " | %value%/%length% MB (%byps% KB/s) | %time%"),
                wrapped
        );
        console.startAnimation(this.progressBarAnimation);
    }

    @Override
    public int read() throws IOException {
        int read = this.wrapped.read();
        this.progressBarAnimation.setCurrentValue(this.progressBarAnimation.getCurrentValue() + 1);
        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int read = this.wrapped.read(b);
        this.progressBarAnimation.setCurrentValue(this.progressBarAnimation.getCurrentValue() + read);
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
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
        super.close();
    }
}
