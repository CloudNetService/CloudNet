package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface IConsole extends AutoCloseable {

    Collection<AbstractConsoleAnimation> getRunningAnimations();

    void startAnimation(AbstractConsoleAnimation animation);

    boolean isAnimationRunning();

    void togglePrinting(boolean enabled);

    boolean isPrintingEnabled();

    default boolean hasAnimationSupport() {
        return this.hasColorSupport();
    }

    List<String> getCommandHistory();

    void setCommandHistory(List<String> history);

    /**
     * @deprecated use {@link #setBuffer(String)} instead
     */
    @Deprecated
    void setCommandInputValue(String commandInputValue);

    ITask<String> readLine();

    void enableAllHandlers();

    void disableAllHandlers();

    void enableAllTabCompletionHandlers();

    void disableAllTabCompletionHandlers();

    void enableAllCommandHandlers();

    void disableAllCommandHandlers();

    void addCommandHandler(UUID uniqueId, Consumer<String> inputConsumer);

    void removeCommandHandler(UUID uniqueId);

    void addTabCompletionHandler(UUID uniqueId, ITabCompleter completer);

    void removeTabCompletionHandler(UUID uniqueId);

    IConsole writeRaw(String rawText);

    IConsole forceWrite(String text);

    IConsole forceWriteLine(String text);

    IConsole write(String text);

    IConsole writeLine(String text);

    IConsole writeDirectly(String text);

    boolean hasColorSupport();

    String readLine(String prompt, String buffer) throws EndOfFileException, UserInterruptException;

    void setPrompt(String prompt);

    String getPrompt();

    void reset();

    void start();

    void resetPrompt();

    void clearScreen();

    void clearScreenAndCache();

    String getBuffer();

    void setBuffer(String buffer);

    String getScreenName();

    void setScreenName(String name);

}