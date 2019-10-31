package de.dytanic.cloudnet.template.install.installer;

import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.stream.ErrorConsoleStreamThread;
import de.dytanic.cloudnet.template.install.stream.InfoConsoleStreamThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

public abstract class ServiceVersionInstaller {

    public abstract void install(ServiceVersion version, Path workingDirectory, OutputStream targetStream) throws IOException;

    protected int startProcessAndWaitFor(String command, Path workingDirectory) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command, null, workingDirectory.toFile());
        CountDownLatch countDownLatch = new CountDownLatch(2);
        new ErrorConsoleStreamThread(process.getErrorStream(), countDownLatch).start();
        new InfoConsoleStreamThread(process.getInputStream(), countDownLatch).start();
        countDownLatch.await();
        return process.waitFor();
    }

}
