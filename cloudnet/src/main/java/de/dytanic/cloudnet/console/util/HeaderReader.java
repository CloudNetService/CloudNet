package de.dytanic.cloudnet.console.util;

import de.dytanic.cloudnet.console.IConsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class HeaderReader {

    private HeaderReader() {
        throw new UnsupportedOperationException();
    }

    public static void readAndPrintHeader(IConsole console) {
        String version = HeaderReader.class.getPackage().getImplementationVersion();
        String codename = HeaderReader.class.getPackage().getImplementationTitle();

        try (InputStream inputStream = HeaderReader.class.getClassLoader().getResourceAsStream("header.txt");
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String input;
            while ((input = bufferedReader.readLine()) != null) {
                console.writeLine(input.replace("%codename%", codename).replace("%version%", version));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}