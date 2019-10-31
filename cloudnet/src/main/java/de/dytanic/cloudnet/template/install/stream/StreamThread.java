package de.dytanic.cloudnet.template.install.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public abstract class StreamThread extends Thread {

    private InputStream inputStream;
    private CountDownLatch countDownLatch;

    public StreamThread(InputStream inputStream, CountDownLatch countDownLatch) {
        super("StreamThread");
        this.inputStream = inputStream;
        this.countDownLatch = countDownLatch;
    }

    protected abstract void print(String line);

    @Override
    public void run() {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                this.print(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.countDownLatch.countDown();
    }
}
