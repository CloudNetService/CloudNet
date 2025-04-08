/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.console;

import eu.cloudnetservice.node.impl.console.animation.AbstractConsoleAnimation;
import eu.cloudnetservice.node.impl.console.handler.ConsoleInputHandler;
import eu.cloudnetservice.node.impl.console.handler.ConsoleTabCompleteHandler;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

public interface Console extends AutoCloseable {

  @UnmodifiableView
  @NonNull
  Collection<AbstractConsoleAnimation> runningAnimations();

  void startAnimation(@NonNull AbstractConsoleAnimation animation);

  boolean animationRunning();

  void togglePrinting(boolean enabled);

  boolean printingEnabled();

  @Unmodifiable
  @NonNull
  Collection<String> commandHistory();

  void commandHistory(@Nullable Collection<String> history);

  void commandInputValue(@NonNull String commandInputValue);

  @NonNull
  CompletableFuture<String> readLine();

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

  @NonNull
  Console writeRaw(@NonNull Supplier<String> rawText);

  @NonNull
  Console forceWriteLine(@NonNull String text);

  @NonNull
  Console writeLine(@NonNull String text);

  boolean usingMatchingHistoryComplete();

  void usingMatchingHistoryComplete(boolean matchingHistoryComplete);

  @NonNull
  String prompt();

  void prompt(@NonNull String prompt);

  void resetPrompt();

  void removePrompt();

  void emptyPrompt();

  void clearScreen();

  int width();

  int displayLength(@NonNull String string);
}
