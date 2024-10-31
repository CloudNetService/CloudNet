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

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.node.impl.Node;
import eu.cloudnetservice.node.impl.console.animation.AbstractConsoleAnimation;
import eu.cloudnetservice.node.impl.console.handler.ConsoleInputHandler;
import eu.cloudnetservice.node.impl.console.handler.ConsoleTabCompleteHandler;
import eu.cloudnetservice.node.impl.console.handler.Toggleable;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jline.jansi.Ansi;
import org.jline.reader.LineReader;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import org.jline.utils.StyleResolver;
import org.jline.utils.WCWidth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Provides(Console.class)
public final class JLine3Console implements Console {

  private static final String USER = System.getProperty("user.name");
  private static final Logger LOGGER = LoggerFactory.getLogger(JLine3Console.class);
  private static final String VERSION = Node.class.getPackage().getImplementationVersion();
  private static final String HISTORY_FILE = System.getProperty("cloudnet.history.file", "local/.consolehistory");

  private final Lock printLock = new ReentrantLock(true);

  private final Map<UUID, ConsoleInputHandler> consoleInputHandler = new ConcurrentHashMap<>();
  private final Map<UUID, ConsoleTabCompleteHandler> tabCompleteHandler = new ConcurrentHashMap<>();

  private final Map<UUID, AbstractConsoleAnimation> runningAnimations = new ConcurrentHashMap<>();

  private final ConsoleReadThread consoleReadThread = new ConsoleReadThread(this);
  private final ExecutorService animationThreadPool = Executors.newCachedThreadPool();

  private final Terminal terminal;
  private final LineReaderImpl lineReader;

  private String prompt = System.getProperty("cloudnet.console.prompt", "&c%user%&r@&7%version% &f=> &r");

  private boolean printingEnabled = true;
  private boolean matchingHistorySearch = true;

  public JLine3Console() throws Exception {
    // disable the logging of jline, as log messages will be redirected into this console
    // which will trigger a log message (when debug is enabled) etc.
    // in short: it triggers a StackOverflow exception when enabling debug logging
    java.util.logging.Logger.getLogger("org.jline").setLevel(Level.OFF);
    java.util.logging.Logger.getLogger(StyleResolver.class.getName()).setLevel(Level.OFF);

    this.terminal = TerminalBuilder.builder().system(true).encoding(StandardCharsets.UTF_8).build();
    this.lineReader = new InternalLineReader(this.terminal);

    this.lineReader.setCompleter(new JLine3Completer(this));
    this.lineReader.setCompletionMatcher(new JLine3CompletionMatcher());
    this.lineReader.setAutosuggestion(LineReader.SuggestionType.COMPLETER);

    this.lineReader.option(LineReader.Option.AUTO_GROUP, false);
    this.lineReader.option(LineReader.Option.AUTO_MENU_LIST, true);
    this.lineReader.option(LineReader.Option.AUTO_FRESH_LINE, true);
    this.lineReader.option(LineReader.Option.EMPTY_WORD_OPTIONS, false);
    this.lineReader.option(LineReader.Option.HISTORY_TIMESTAMPED, false);
    this.lineReader.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true);

    this.lineReader.variable(LineReader.BELL_STYLE, "none");
    this.lineReader.variable(LineReader.HISTORY_SIZE, 500);
    this.lineReader.variable(LineReader.HISTORY_FILE_SIZE, 2500);
    this.lineReader.variable(LineReader.HISTORY_FILE, Path.of(HISTORY_FILE));
    this.lineReader.variable(LineReader.COMPLETION_STYLE_LIST_BACKGROUND, "inverse");

    this.updatePrompt();
    this.consoleReadThread.start();
  }

  @Override
  public @NonNull Collection<AbstractConsoleAnimation> runningAnimations() {
    return this.runningAnimations.values();
  }

  @Override
  public void startAnimation(@NonNull AbstractConsoleAnimation animation) {
    animation.console(this);

    var uniqueId = UUID.randomUUID();
    this.runningAnimations.put(uniqueId, animation);

    this.animationThreadPool.execute(() -> {
      animation.run();
      // remove the animation - then post the result for other animations in the finish handlers to run
      this.runningAnimations.remove(uniqueId);
      // animation done - reset the console
      animation.resetConsole();
      animation.handleDone();
    });
  }

  @Override
  public boolean animationRunning() {
    return !this.runningAnimations.isEmpty();
  }

  @Override
  public void togglePrinting(boolean enabled) {
    this.printingEnabled = enabled;
  }

  @Override
  public boolean printingEnabled() {
    return this.printingEnabled;
  }

  @Override
  public @NonNull Collection<String> commandHistory() {
    List<String> result = new ArrayList<>();
    for (var entry : this.lineReader.getHistory()) {
      result.add(entry.line());
    }

    return result;
  }

  @Override
  public void commandHistory(@Nullable Collection<String> history) {
    try {
      this.lineReader.getHistory().purge();
    } catch (IOException exception) {
      LOGGER.error("Exception while purging the console history", exception);
    }

    if (history != null) {
      for (var s : history) {
        this.lineReader.getHistory().add(s);
      }
    }
  }

  @Override
  public void commandInputValue(@NonNull String commandInputValue) {
    this.lineReader.getBuffer().write(commandInputValue);
  }

  @Override
  public @NonNull CompletableFuture<String> readLine() {
    return this.consoleReadThread.currentTask();
  }

  @Override
  public void enableAllHandlers() {
    this.enableAllCommandHandlers();
    this.enableAllTabCompleteHandlers();
  }

  @Override
  public void disableAllHandlers() {
    this.disableAllCommandHandlers();
    this.disableAllTabCompleteHandlers();
  }

  @Override
  public void enableAllCommandHandlers() {
    this.toggleHandlers(true, this.consoleInputHandler.values());
  }

  @Override
  public void disableAllCommandHandlers() {
    this.toggleHandlers(false, this.consoleInputHandler.values());
  }

  @Override
  public void enableAllTabCompleteHandlers() {
    this.toggleHandlers(true, this.tabCompleteHandler.values());
  }

  @Override
  public void disableAllTabCompleteHandlers() {
    this.toggleHandlers(false, this.tabCompleteHandler.values());
  }

  @Override
  public void addCommandHandler(@NonNull UUID uniqueId, @NonNull ConsoleInputHandler handler) {
    this.consoleInputHandler.put(uniqueId, handler);
  }

  @Override
  public void removeCommandHandler(@NonNull UUID uniqueId) {
    this.consoleInputHandler.remove(uniqueId);
  }

  @Override
  public void addTabCompleteHandler(@NonNull UUID uniqueId, @NonNull ConsoleTabCompleteHandler handler) {
    this.tabCompleteHandler.put(uniqueId, handler);
  }

  @Override
  public void removeTabCompleteHandler(@NonNull UUID uniqueId) {
    this.tabCompleteHandler.remove(uniqueId);
  }

  @Override
  public @NonNull Console writeRaw(@NonNull Supplier<String> rawText) {
    this.printLock.lock();
    try {
      this.print(this.formatText(rawText.get(), ""));
      return this;
    } finally {
      this.printLock.unlock();
    }
  }

  @Override
  public @NonNull Console writeLine(@NonNull String text) {
    if (this.printingEnabled) {
      this.forceWriteLine(text);
    }

    return this;
  }

  @Override
  public @NonNull Console forceWriteLine(@NonNull String text) {
    this.printLock.lock();
    try {
      // ensure that the given text is formatted properly
      var content = this.formatText(text, System.lineSeparator());
      this.print(Ansi.ansi().eraseLine(Ansi.Erase.ALL).toString() + '\r' + content + Ansi.ansi().reset().toString());

      // increases the amount of lines the running animations is off the current printed lines
      if (!this.runningAnimations.isEmpty()) {
        for (var animation : this.runningAnimations.values()) {
          animation.addToCursor(1);
        }
      }
    } finally {
      this.printLock.unlock();
    }

    return this;
  }

  @Override
  public boolean usingMatchingHistoryComplete() {
    return this.matchingHistorySearch;
  }

  @Override
  public void usingMatchingHistoryComplete(boolean matchingHistoryComplete) {
    this.matchingHistorySearch = matchingHistoryComplete;
  }

  @Override
  public void resetPrompt() {
    this.prompt = System.getProperty("cloudnet.console.prompt", "&c%user%&r@&7%version% &f=> &r");
    this.updatePrompt();
  }

  @Override
  public void removePrompt() {
    this.prompt = null;
    this.updatePrompt();
  }

  @Override
  public void emptyPrompt() {
    this.prompt = ConsoleColor.DEFAULT.toString();
    this.updatePrompt();
  }

  @Override
  public void clearScreen() {
    this.terminal.puts(InfoCmp.Capability.clear_screen);
    this.terminal.flush();
    this.redisplay();
  }

  @Override
  public void close() throws Exception {
    this.animationThreadPool.shutdownNow();
    this.consoleReadThread.interrupt();

    this.terminal.flush();
    this.terminal.close();
  }

  @Override
  public @NonNull String prompt() {
    return this.prompt;
  }

  @Override
  public void prompt(@NonNull String prompt) {
    this.prompt = prompt;
    this.updatePrompt();
  }

  @Override
  public int width() {
    return this.terminal.getWidth();
  }

  @Override
  public int displayLength(@NonNull String string) {
    var result = 0;
    // count for the length of each char in the string
    for (var i = 0; i < string.length(); i++) {
      result += Math.max(0, WCWidth.wcwidth(string.charAt(i)));
    }
    return result;
  }

  private void updatePrompt() {
    this.prompt = ConsoleColor.toColoredString('&', this.prompt)
      .replace("%version%", VERSION)
      .replace("%user%", USER);
    this.lineReader.setPrompt(this.prompt);
  }

  private void print(@NonNull String text) {
    // print out the raw given line
    this.lineReader.getTerminal().puts(InfoCmp.Capability.carriage_return);
    this.lineReader.getTerminal().puts(InfoCmp.Capability.clr_eol);
    this.lineReader.getTerminal().writer().print(text);
    this.lineReader.getTerminal().writer().flush();

    // re-displays the prompt to ensure everything is lined up
    this.redisplay();
  }

  private void redisplay() {
    if (this.lineReader.isReading()) {
      this.lineReader.callWidget(LineReader.REDRAW_LINE);
      this.lineReader.callWidget(LineReader.REDISPLAY);
    }
  }

  private void toggleHandlers(boolean enabled, @NonNull Collection<? extends Toggleable> handlers) {
    for (var handler : handlers) {
      handler.enabled(enabled);
    }
  }

  private @NonNull String formatText(@NonNull String input, @NonNull String ensureEndsWith) {
    var content = ConsoleColor.toColoredString('&', input);
    if (!content.endsWith(ensureEndsWith)) {
      content += ensureEndsWith;
    }

    return content;
  }

  @ApiStatus.Internal
  @NonNull
  LineReader lineReader() {
    return this.lineReader;
  }

  @ApiStatus.Internal
  @NonNull
  Map<UUID, ConsoleInputHandler> consoleInputHandler() {
    return this.consoleInputHandler;
  }

  @ApiStatus.Internal
  @NonNull
  Map<UUID, ConsoleTabCompleteHandler> tabCompleteHandlers() {
    return this.tabCompleteHandler;
  }

  private final class InternalLineReader extends LineReaderImpl {

    private InternalLineReader(@NonNull Terminal terminal) {
      super(terminal, "CloudNet-Console", null);
    }

    @Override
    protected boolean historySearchBackward() {
      if (JLine3Console.this.usingMatchingHistoryComplete()) {
        return super.historySearchBackward();
      }

      if (this.history.previous()) {
        this.setBuffer(this.history.current());
        return true;
      } else {
        return false;
      }
    }

    @Override
    protected boolean historySearchForward() {
      if (JLine3Console.this.usingMatchingHistoryComplete()) {
        return super.historySearchForward();
      }

      if (this.history.next()) {
        this.setBuffer(this.history.current());
        return true;
      } else {
        return false;
      }
    }

    @Override
    protected boolean upLineOrSearch() {
      return this.historySearchBackward();
    }

    @Override
    protected boolean downLineOrSearch() {
      return this.historySearchForward();
    }
  }
}
