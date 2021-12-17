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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

public interface IConsole extends AutoCloseable {

  @NonNull
  @UnmodifiableView
  Collection<AbstractConsoleAnimation> runningAnimations();

  void startAnimation(@NonNull AbstractConsoleAnimation animation);

  boolean animationRunning();

  void togglePrinting(boolean enabled);

  boolean printingEnabled();

  default boolean hasAnimationSupport() {
    return this.hasColorSupport();
  }

  @Unmodifiable
  @NonNull Collection<String> commandHistory();

  void commandHistory(@Nullable Collection<String> history);

  void commandInputValue(@NonNull String commandInputValue);

  @NonNull
  ITask<String> readLine();

  void enableAllHandlers();

  void disableAllHandlers();

  void enableAllCommandHandlers();

  void disableAllCommandHandlers();

  void enableAllTabCompleteHandlers();

  void disableAllTabCompleteHandlers();

  void addCommandHandler(@NonNull UUID uniqueId, @NonNull ConsoleInputHandler handler);

  void removeCommandHandler(@NonNull UUID uniqueId);

  void addTabCompleteHandler(@NonNull UUID uniqueId, @NonNull ConsoleTabCompleteHandler handler);

  void removeTabCompleteHandler(@NonNull UUID uniqueId);

  @NonNull IConsole writeRaw(@NonNull String rawText);

  @NonNull IConsole forceWrite(@NonNull String text);

  @NonNull IConsole forceWriteLine(@NonNull String text);

  @NonNull IConsole write(@NonNull String text);

  @NonNull IConsole writeLine(@NonNull String text);

  boolean hasColorSupport();

  boolean usingMatchingHistoryComplete();

  void usingMatchingHistoryComplete(boolean matchingHistoryComplete);

  @NonNull String prompt();

  void prompt(@NonNull String prompt);

  void resetPrompt();

  void removePrompt();

  void emptyPrompt();

  void clearScreen();

  @NonNull String screenName();

  void screenName(@NonNull String name);

  int width();

  int displayLength(@NonNull String string);
}
