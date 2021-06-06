package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

public interface IConsole extends AutoCloseable {

  @NotNull
  @UnmodifiableView
  Collection<AbstractConsoleAnimation> getRunningAnimations();

  void startAnimation(@NotNull AbstractConsoleAnimation animation);

  boolean isAnimationRunning();

  void togglePrinting(boolean enabled);

  boolean isPrintingEnabled();

  default boolean hasAnimationSupport() {
    return this.hasColorSupport();
  }

  @NotNull @Unmodifiable List<String> getCommandHistory();

  void setCommandHistory(@Nullable List<String> history);

  void setCommandInputValue(@NotNull String commandInputValue);

  @NotNull
  ITask<String> readLine();

  void enableAllHandlers();

  void disableAllHandlers();

  void enableAllTabCompletionHandlers();

  void disableAllTabCompletionHandlers();

  void enableAllCommandHandlers();

  void disableAllCommandHandlers();

  void addCommandHandler(@NotNull UUID uniqueId, @NotNull Consumer<String> inputConsumer);

  void removeCommandHandler(@NotNull UUID uniqueId);

  void addTabCompletionHandler(@NotNull UUID uniqueId, @NotNull ITabCompleter completer);

  void removeTabCompletionHandler(@NotNull UUID uniqueId);

  @NotNull IConsole writeRaw(@NotNull String rawText);

  @NotNull IConsole forceWrite(@NotNull String text);

  @NotNull IConsole forceWriteLine(@NotNull String text);

  @NotNull IConsole write(@NotNull String text);

  @NotNull IConsole writeLine(@NotNull String text);

  boolean hasColorSupport();

  boolean isUsingMatchingHistoryComplete();

  void setUsingMatchingHistoryComplete(boolean matchingHistoryComplete);

  @NotNull String getPrompt();

  void setPrompt(@NotNull String prompt);

  void resetPrompt();

  void removePrompt();

  void emptyPrompt();

  void clearScreen();

  @NotNull String getScreenName();

  void setScreenName(@NotNull String name);

}
