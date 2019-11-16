package de.dytanic.cloudnet.template.install.installer.processing;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ProcessingInstallerOutput implements Runnable {
    private InputStream inputStream;
    private PrintStream outputStream;

    public ProcessingInstallerOutput(InputStream inputStream, PrintStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                this.outputStream.println(line);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

}
