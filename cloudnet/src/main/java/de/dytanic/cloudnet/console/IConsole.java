package de.dytanic.cloudnet.console;

public interface IConsole extends AutoCloseable {

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