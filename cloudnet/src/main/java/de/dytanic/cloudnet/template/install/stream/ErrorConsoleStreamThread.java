package de.dytanic.cloudnet.template.install.stream;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

public class ErrorConsoleStreamThread extends StreamThread {
    public ErrorConsoleStreamThread(InputStream inputStream, CountDownLatch countDownLatch) {
        super(inputStream, countDownLatch);
    }

    @Override
    protected void print(String line) {
        System.err.println(line);
    }
}
