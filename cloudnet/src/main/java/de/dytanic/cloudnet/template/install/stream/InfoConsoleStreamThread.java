package de.dytanic.cloudnet.template.install.stream;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

public class InfoConsoleStreamThread extends StreamThread {
    public InfoConsoleStreamThread(InputStream inputStream, CountDownLatch countDownLatch) {
        super(inputStream, countDownLatch);
    }

    @Override
    protected void print(String line) {
        System.out.println(line);
    }
}
