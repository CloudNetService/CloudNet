package de.dytanic.cloudnet.console;

import jline.console.ConsoleReader;
import org.fusesource.jansi.AnsiConsole;

public final class JLine2Console implements IConsole {

    private final ConsoleReader consoleReader;

    private final String
            user = System.getProperty("user.name"),
            version = System.getProperty("cloudnet.launcher.select.version"),
            prompt = System.getProperty("cloudnet.console.prompt", "&3%user%&0@&9%screen% &1=> ");

    private String screenName = version;

    public JLine2Console() throws Exception {
        AnsiConsole.systemInstall();

        this.consoleReader = new ConsoleReader();
        this.consoleReader.setExpandEvents(false);
    }

    @Override
    public String readLine() throws Exception {
        this.resetPrompt();
        String input = this.consoleReader.readLine(
                ConsoleColor.toColouredString('&', prompt)
                        .replace("%version%", version)
                        .replace("%screen%", screenName + "")
                        .replace("%user%", user)
                        + ConsoleColor.DEFAULT
        );

        this.resetPrompt();

        return input;
    }

    @Override
    public String readLineNoPrompt() throws Exception {
        return this.consoleReader.readLine();
    }

    @Override
    public IConsole write(String text) {
        if (text == null) return this;

        text = ConsoleColor.toColouredString('&', text);

        try {
            this.consoleReader.print(ConsoleReader.RESET_LINE + text + ConsoleColor.DEFAULT);
            this.consoleReader.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return this;
    }

    @Override
    public IConsole writeLine(String text) {
        if (text == null) return this;

        text = ConsoleColor.toColouredString('&', text);

        if (!text.endsWith(System.lineSeparator()))
            text = text + System.lineSeparator();

        try {
            this.consoleReader.print(ConsoleReader.RESET_LINE + text + ConsoleColor.DEFAULT);
            this.consoleReader.drawLine();
            this.consoleReader.flush();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return this;
    }

    @Override
    public boolean hasColorSupport() {
        return !(this.consoleReader.getTerminal() instanceof jline.UnsupportedTerminal);
    }

    @Override
    public void setPrompt(String prompt) {
        this.consoleReader.setPrompt(prompt);
    }

    @Override
    public void resetPrompt() {
        this.consoleReader.setPrompt("");
    }

    @Override
    public void close() throws Exception {
        this.consoleReader.close();
    }

    public ConsoleReader getConsoleReader() {
        return this.consoleReader;
    }

    public String getUser() {
        return this.user;
    }

    public String getVersion() {
        return this.version;
    }

    public String getPrompt() {
        return this.prompt;
    }

    public String getScreenName() {
        return this.screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }
}