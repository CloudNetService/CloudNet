package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import jline.console.ConsoleReader;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public final class JLine2Console implements IConsole {

    private final ConsoleReader consoleReader;

    private final String
            user = System.getProperty("user.name"),
            version = System.getProperty("cloudnet.launcher.select.version"),
            prompt = System.getProperty("cloudnet.console.prompt", "&c%user%&r@&7%screen% &f=> ");

    private String screenName = version;

    private AbstractConsoleAnimation runningAnimation;
    private Thread currentAnimationThread;

    public JLine2Console() throws Exception {
        AnsiConsole.systemInstall();

        this.consoleReader = new ConsoleReader();
        this.consoleReader.setExpandEvents(false);
    }

    @Override
    public AbstractConsoleAnimation getRunningAnimation() {
        return this.runningAnimation;
    }

    @Override
    public void startAnimation(AbstractConsoleAnimation animation) {
        Validate.checkNotNull(animation);
        if (this.runningAnimation != null) {
            throw new IllegalStateException("Cannot run multiple animations at once");
        }

        this.runningAnimation = animation;

        if (this.currentAnimationThread != null) {
            this.currentAnimationThread.interrupt();
        }

        animation.setConsole(this);

        this.currentAnimationThread = new Thread(() -> {
            animation.run();
            this.runningAnimation = null;
            this.currentAnimationThread = null;
        }, "Animation - " + animation.getClass().getName());
        this.currentAnimationThread.start();
    }

    @Override
    public boolean isAnimationRunning() {
        return this.runningAnimation != null;
    }

    @Override
    public String readLine() throws Exception {
        this.resetPrompt();

        String input = this.consoleReader.readLine(
                ConsoleColor.toColouredString('&', prompt)
                        .replace("%version%", version)
                        .replace("%screen%", screenName)
                        .replace("%user%", user)
                        + ConsoleColor.DEFAULT
        );

        this.resetPrompt();

        if (this.runningAnimation != null) {
            this.runningAnimation.addToCursor(1);
        }

        return input;
    }

    @Override
    public String readLineNoPrompt() throws Exception {
        return this.consoleReader.readLine();
    }

    @Override
    public IConsole write(String text) {
        if (text == null) {
            return this;
        }

        text = ConsoleColor.toColouredString('&', text);

        try {
            this.consoleReader.print(Ansi.ansi().eraseLine(Ansi.Erase.ALL).toString() + ConsoleReader.RESET_LINE + text + ConsoleColor.DEFAULT);
            this.consoleReader.flush();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return this;
    }

    @Override
    public IConsole writeLine(String text) {
        if (text == null) {
            return this;
        }

        text = ConsoleColor.toColouredString('&', text);

        if (!text.endsWith(System.lineSeparator())) {
            text = text + System.lineSeparator();
        }

        try {
            this.consoleReader.print(Ansi.ansi().eraseLine(Ansi.Erase.ALL).toString() + ConsoleReader.RESET_LINE + text + ConsoleColor.DEFAULT);
            this.consoleReader.drawLine();
            this.consoleReader.flush();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        if (this.runningAnimation != null) {
            this.runningAnimation.addToCursor(1);
        }

        return this;
    }

    @Override
    public boolean hasColorSupport() {
        return !(this.consoleReader.getTerminal() instanceof jline.UnsupportedTerminal);
    }

    @Override
    public void resetPrompt() {
        this.consoleReader.setPrompt("");
    }

    @Override
    public void close() {
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

    @Override
    public void setPrompt(String prompt) {
        this.consoleReader.setPrompt(prompt);
    }

    public String getScreenName() {
        return this.screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }
}
