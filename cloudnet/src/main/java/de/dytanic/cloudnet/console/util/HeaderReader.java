package de.dytanic.cloudnet.console.util;

import de.dytanic.cloudnet.console.IConsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class HeaderReader {

    private HeaderReader() {
        throw new UnsupportedOperationException();
    }

    public static void readAndPrintHeader(IConsole console) {
        String version = HeaderReader.class.getPackage().getImplementationVersion();
        String codename = HeaderReader.class.getPackage().getImplementationTitle();

        InputStream inputStream = HeaderReader.class.getClassLoader().getResourceAsStream("header.txt");
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8))) {
            String input;
            while ((input = bufferedReader.readLine()) != null) {
                console.writeLine(input.replace("%codename%", codename).replace("%version%", version));
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}