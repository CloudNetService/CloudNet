package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

public class ConsoleReadThread extends Thread {

  private final JLine3Console console;
  private CompletableTask<String> currentTask;

  public ConsoleReadThread(JLine3Console console) {
    this.console = console;
  }

  @Override
  public void run() {
    String line;
    while (!Thread.interrupted() && (line = this.readLine()) != null) {
      if (this.currentTask != null) {
        this.currentTask.complete(line);
        this.currentTask = null;
      }

      for (ConsoleHandler<Consumer<String>> value : this.console.getConsoleInputHandler().values()) {
        if (value.isEnabled()) {
          value.getHandler().accept(line);
        }
      }

      for (AbstractConsoleAnimation animation : this.console.getRunningAnimations()) {
        animation.addToCursor(1);
      }
    }
  }

  private @Nullable String readLine() {
    try {
      return this.console.getLineReader().readLine(this.console.getPrompt());
    } catch (EndOfFileException ignored) {
    } catch (UserInterruptException exception) {
      System.exit(-1);
    }

    return null;
  }

  protected @NotNull ITask<String> getCurrentTask() {
    if (this.currentTask == null) {
      this.currentTask = new CompletableTask<>();
    }

    return this.currentTask;
  }
}
