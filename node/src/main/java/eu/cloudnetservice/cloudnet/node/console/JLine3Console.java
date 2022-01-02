/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.console;

import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.console.animation.AbstractConsoleAnimation;
import eu.cloudnetservice.cloudnet.node.console.handler.ConsoleInputHandler;
import eu.cloudnetservice.cloudnet.node.console.handler.ConsoleTabCompleteHandler;
import eu.cloudnetservice.cloudnet.node.console.handler.Toggleable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.NonNull;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import org.jline.utils.WCWidth;

public final class JLine3Console implements Console {

  private static final String USER = System.getProperty("user.name");
  private static final String VERSION = CloudNet.class.getPackage().getImplementationVersion();
  private static final Logger LOGGER = LogManager.logger(JLine3Console.class);

  private final Map<UUID, ConsoleInputHandler> consoleInputHandler = new ConcurrentHashMap<>();
  private final Map<UUID, ConsoleTabCompleteHandler> tabCompleteHandler = new ConcurrentHashMap<>();

  private final Map<UUID, AbstractConsoleAnimation> runningAnimations = new ConcurrentHashMap<>();

  private final ConsoleReadThread consoleReadThread = new ConsoleReadThread(this);
  private final ExecutorService animationThreadPool = Executors.newCachedThreadPool();

  private final Terminal terminal;
  private final LineReaderImpl lineReader;

  private String prompt = System.getProperty("cloudnet.console.prompt", "&c%user%&r@&7%screen% &f=> &r");
  private String screenName = VERSION;
  private boolean printingEnabled = true;
  private boolean matchingHistorySearch = true;

  public JLine3Console() throws Exception {
    System.setProperty("library.jansi.version", "CloudNET");

    try {
      AnsiConsole.systemInstall();
    } catch (Throwable ignored) {
    }

    this.terminal = TerminalBuilder.builder().system(true).encoding(StandardCharsets.UTF_8).build();
    this.lineReader = new InternalLineReaderBuilder(this.terminal)
      .completer(new JLine3Completer(this))
      .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
      .variable(LineReader.BELL_STYLE, "off")
      .build();

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
      animation.handleDone();
      // animation done - reset the console
      animation.resetConsole();
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
      LOGGER.severe("Exception while purging the console history", exception);
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
  @NonNull
  public Task<String> readLine() {
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
  public @NonNull Console write(@NonNull String text) {
    if (this.printingEnabled) {
      this.forceWrite(text);
    }

    return this;
  }

  @Override
  public @NonNull Console writeLine(@NonNull String text) {
    if (this.printingEnabled) {
      this.forceWriteLine(text);
    }

    return this;
  }

  @Override
  public @NonNull Console forceWrite(@NonNull String text) {
    return this.writeRaw(Ansi.ansi().eraseLine(Ansi.Erase.ALL).toString() + '\r' + text + ConsoleColor.DEFAULT);
  }

  @Override
  public @NonNull Console writeRaw(@NonNull String rawText) {
    this.print(ConsoleColor.toColouredString('&', rawText));
    return this;
  }

  @Override
  public @NonNull Console forceWriteLine(@NonNull String text) {
    text = ConsoleColor.toColouredString('&', text);
    if (!text.endsWith(System.lineSeparator())) {
      text += System.lineSeparator();
    }

    this.print(Ansi.ansi().eraseLine(Ansi.Erase.ALL).toString() + '\r' + text + Ansi.ansi().reset().toString());
    if (!this.runningAnimations.isEmpty()) {
      for (var animation : this.runningAnimations.values()) {
        animation.addToCursor(1);
      }
    }

    return this;
  }

  @Override
  public boolean hasColorSupport() {
    return true;
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
    this.prompt = System.getProperty("cloudnet.console.prompt", "&c%user%&r@&7%screen% &f=> &r");
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
  }

  @Override
  public void close() throws Exception {
    this.animationThreadPool.shutdownNow();
    this.consoleReadThread.interrupt();

    this.terminal.flush();
    this.terminal.close();

    AnsiConsole.systemUninstall();
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
  public @NonNull String screenName() {
    return this.screenName;
  }

  @Override
  public void screenName(@NonNull String screenName) {
    this.screenName = screenName;
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
      result += Math.max(WCWidth.wcwidth(string.charAt(i)), 0);
    }
    return result;
  }

  private void updatePrompt() {
    this.prompt = ConsoleColor.toColouredString('&', this.prompt)
      .replace("%version%", VERSION)
      .replace("%user%", USER)
      .replace("%screen%", this.screenName);
    this.lineReader.setPrompt(this.prompt);
  }

  private void print(@NonNull String text) {
    this.lineReader.getTerminal().puts(InfoCmp.Capability.carriage_return);
    this.lineReader.getTerminal().puts(InfoCmp.Capability.clr_eol);
    this.lineReader.getTerminal().writer().print(text);
    this.lineReader.getTerminal().writer().flush();

    this.redisplay();
  }

  private void redisplay() {
    if (!this.lineReader.isReading()) {
      return;
    }

    this.lineReader.callWidget(LineReader.REDRAW_LINE);
    this.lineReader.callWidget(LineReader.REDISPLAY);
  }

  private void toggleHandlers(boolean enabled, @NonNull Collection<?> handlers) {
    for (Object handler : handlers) {
      ((Toggleable) handler).enabled(enabled);
    }
  }

  @NonNull
  @Internal
  LineReader lineReader() {
    return this.lineReader;
  }

  @NonNull
  @Internal
  Map<UUID, ConsoleInputHandler> consoleInputHandler() {
    return this.consoleInputHandler;
  }

  @NonNull
  @Internal
  Map<UUID, ConsoleTabCompleteHandler> tabCompleteHandlers() {
    return this.tabCompleteHandler;
  }

  private final class InternalLineReader extends LineReaderImpl {

    private InternalLineReader(Terminal terminal, String appName, Map<String, Object> variables) {
      super(terminal, appName, variables);
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

  private final class InternalLineReaderBuilder {

    private final Terminal terminal;
    private final Map<String, Object> variables = new HashMap<>();
    private final Map<LineReader.Option, Boolean> options = new HashMap<>();
    private Completer completer;

    private InternalLineReaderBuilder(@NonNull Terminal terminal) {
      this.terminal = terminal;
    }

    public @NonNull InternalLineReaderBuilder variable(@NonNull String name, @NonNull Object value) {
      this.variables.put(name, value);
      return this;
    }

    public @NonNull InternalLineReaderBuilder option(@NonNull LineReader.Option option, boolean value) {
      this.options.put(option, value);
      return this;
    }

    public @NonNull InternalLineReaderBuilder completer(@NonNull Completer completer) {
      this.completer = completer;
      return this;
    }

    public @NonNull InternalLineReader build() {
      var reader = new InternalLineReader(this.terminal, "CloudNet-Console", this.variables);
      if (this.completer != null) {
        reader.setCompleter(this.completer);
      }

      for (var e : this.options.entrySet()) {
        reader.option(e.getKey(), e.getValue());
      }

      return reader;
    }
  }
}
