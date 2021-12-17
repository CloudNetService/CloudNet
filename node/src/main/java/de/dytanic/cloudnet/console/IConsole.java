/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import de.dytanic.cloudnet.console.handler.ConsoleInputHandler;
import de.dytanic.cloudnet.console.handler.ConsoleTabCompleteHandler;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

public interface IConsole extends AutoCloseable {

  @NotNull
  @UnmodifiableView
  Collection<AbstractConsoleAnimation> runningAnimations();

  void startAnimation(@NotNull AbstractConsoleAnimation animation);

  boolean animationRunning();

  void togglePrinting(boolean enabled);

  boolean printingEnabled();

  default boolean hasAnimationSupport() {
    return this.hasColorSupport();
  }

  @Unmodifiable
  @NotNull Collection<String> commandHistory();

  void commandHistory(@Nullable Collection<String> history);

  void commandInputValue(@NotNull String commandInputValue);

  @NotNull
  ITask<String> readLine();

  void enableAllHandlers();

  void disableAllHandlers();

  void enableAllCommandHandlers();

  void disableAllCommandHandlers();

  void enableAllTabCompleteHandlers();

  void disableAllTabCompleteHandlers();

  void addCommandHandler(@NotNull UUID uniqueId, @NotNull ConsoleInputHandler handler);

  void removeCommandHandler(@NotNull UUID uniqueId);

  void addTabCompleteHandler(@NotNull UUID uniqueId, @NotNull ConsoleTabCompleteHandler handler);

  void removeTabCompleteHandler(@NotNull UUID uniqueId);

  @NotNull IConsole writeRaw(@NotNull String rawText);

  @NotNull IConsole forceWrite(@NotNull String text);

  @NotNull IConsole forceWriteLine(@NotNull String text);

  @NotNull IConsole write(@NotNull String text);

  @NotNull IConsole writeLine(@NotNull String text);

  boolean hasColorSupport();

  boolean usingMatchingHistoryComplete();

  void usingMatchingHistoryComplete(boolean matchingHistoryComplete);

  @NotNull String prompt();

  void prompt(@NotNull String prompt);

  void resetPrompt();

  void removePrompt();

  void emptyPrompt();

  void clearScreen();

  @NotNull String screenName();

  void screenName(@NotNull String name);

  int width();

  int displayLength(@NotNull String string);
}
