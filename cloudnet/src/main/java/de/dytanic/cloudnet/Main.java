package de.dytanic.cloudnet;

import de.dytanic.cloudnet.common.language.LanguageManager;

import java.util.Arrays;

public final class Main {

    private Main() {
        throw new UnsupportedOperationException();
    }

    public static synchronized void main(String... args) throws Throwable {
        LanguageManager.setLanguage(System.getProperty("cloudnet.messages.language", "english"));
        LanguageManager.addLanguageFile("german", Main.class.getClassLoader().getResourceAsStream("lang/german.properties"));
        LanguageManager.addLanguageFile("english", Main.class.getClassLoader().getResourceAsStream("lang/english.properties"));

        CloudNet cloudNet = new CloudNet(Arrays.asList(args));
        cloudNet.start();
    }
}
