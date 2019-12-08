package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;

public interface IConsole extends AutoCloseable {

    AbstractConsoleAnimation getRunningAnimation();

    void startAnimation(AbstractConsoleAnimation animation);

    boolean isAnimationRunning();

    default boolean hasAnimationSupport() {
        return this.hasColorSupport();
    }

    String readLine() throws Exception;

    String readLineNoPrompt() throws Exception;

    IConsole write(String text);

    IConsole writeLine(String text);

    boolean hasColorSupport();

    void setPrompt(String prompt);

    void resetPrompt();

    String getScreenName();

    void setScreenName(String name);

}