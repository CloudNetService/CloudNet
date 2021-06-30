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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.Completer;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

public final class JLine3Console implements IConsole {

  private static final String USER = System.getProperty("user.name");
  private static final String VERSION = CloudNet.class.getPackage().getImplementationVersion();

  private final Map<UUID, ConsoleHandler<Consumer<String>>> consoleInputHandler = new ConcurrentHashMap<>();
  private final Map<UUID, ConsoleHandler<ITabCompleter>> tabCompletionHandler = new ConcurrentHashMap<>();
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
  public @NotNull Collection<AbstractConsoleAnimation> getRunningAnimations() {
    return this.runningAnimations.values();
  }

  @Override
  public void startAnimation(@NotNull AbstractConsoleAnimation animation) {
    animation.setConsole(this);

    UUID uniqueId = UUID.randomUUID();
    this.runningAnimations.put(uniqueId, animation);

    this.animationThreadPool.execute(() -> {
      animation.run();
      this.runningAnimations.remove(uniqueId);
    });
  }

  @Override
  public boolean isAnimationRunning() {
    return !this.runningAnimations.isEmpty();
  }

  @Override
  public void togglePrinting(boolean enabled) {
    this.printingEnabled = enabled;
  }

  @Override
  public boolean isPrintingEnabled() {
    return this.printingEnabled;
  }

  @Override
  public @NotNull List<String> getCommandHistory() {
    List<String> result = new ArrayList<>();
    for (History.Entry entry : this.lineReader.getHistory()) {
      result.add(entry.line());
    }

    return result;
  }

  @Override
  public void setCommandHistory(List<String> history) {
    try {
      this.lineReader.getHistory().purge();
    } catch (IOException exception) {
      exception.printStackTrace();
    }

    if (history != null) {
      for (String s : history) {
        this.lineReader.getHistory().add(s);
      }
    }
  }

  @Override
  public void setCommandInputValue(@NotNull String commandInputValue) {
    this.lineReader.getBuffer().write(commandInputValue);
  }

  @Override
  @NotNull
  public ITask<String> readLine() {
    return this.consoleReadThread.getCurrentTask();
  }

  @Override
  public void enableAllHandlers() {
    this.enableAllCommandHandlers();
    this.enableAllTabCompletionHandlers();
  }

  @Override
  public void disableAllHandlers() {
    this.disableAllCommandHandlers();
    this.disableAllTabCompletionHandlers();
  }

  @Override
  public void enableAllTabCompletionHandlers() {
    this.toggleHandlers(true, this.tabCompletionHandler.values());
  }

  @Override
  public void disableAllTabCompletionHandlers() {
    this.toggleHandlers(false, this.tabCompletionHandler.values());
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
  public void addCommandHandler(@NotNull UUID uniqueId, @NotNull Consumer<String> inputConsumer) {
    this.consoleInputHandler.put(uniqueId, new ConsoleHandler<>(inputConsumer));
  }

  @Override
  public void removeCommandHandler(@NotNull UUID uniqueId) {
    this.consoleInputHandler.remove(uniqueId);
  }

  @Override
  public void addTabCompletionHandler(@NotNull UUID uniqueId, @NotNull ITabCompleter completer) {
    this.tabCompletionHandler.put(uniqueId, new ConsoleHandler<>(completer));
  }

  @Override
  public void removeTabCompletionHandler(@NotNull UUID uniqueId) {
    this.tabCompletionHandler.remove(uniqueId);
  }

  @Override
  public @NotNull IConsole write(@NotNull String text) {
    if (this.printingEnabled) {
      this.forceWrite(text);
    }

    return this;
  }

  @Override
  public @NotNull IConsole writeLine(@NotNull String text) {
    if (this.printingEnabled) {
      this.forceWriteLine(text);
    }

    return this;
  }

  @Override
  public @NotNull IConsole forceWrite(@NotNull String text) {
    return this.writeRaw(Ansi.ansi().eraseLine(Ansi.Erase.ALL).toString() + '\r' + text + ConsoleColor.DEFAULT);
  }

  @Override
  public @NotNull IConsole writeRaw(@NotNull String rawText) {
    this.print(ConsoleColor.toColouredString('&', rawText));
    return this;
  }

  @Override
  public @NotNull IConsole forceWriteLine(@NotNull String text) {
    text = ConsoleColor.toColouredString('&', text);
    if (!text.endsWith(System.lineSeparator())) {
      text += System.lineSeparator();
    }

    this.print(Ansi.ansi().eraseLine(Ansi.Erase.ALL).toString() + '\r' + text + Ansi.ansi().reset().toString());
    if (!this.runningAnimations.isEmpty()) {
      for (AbstractConsoleAnimation animation : this.runningAnimations.values()) {
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
  public boolean isUsingMatchingHistoryComplete() {
    return this.matchingHistorySearch;
  }

  @Override
  public void setUsingMatchingHistoryComplete(boolean matchingHistoryComplete) {
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
  public @NotNull String getPrompt() {
    return this.prompt;
  }

  @Override
  public void setPrompt(@NotNull String prompt) {
    this.prompt = prompt;
    this.updatePrompt();
  }

  @Override
  public @NotNull String getScreenName() {
    return this.screenName;
  }

  @Override
  public void setScreenName(@NotNull String screenName) {
    this.screenName = screenName;
  }

  private void updatePrompt() {
    this.prompt = ConsoleColor.toColouredString('&', this.prompt)
      .replace("%version%", VERSION)
      .replace("%user%", USER)
      .replace("%screen%", this.screenName);
    this.lineReader.setPrompt(this.prompt);
  }

  private void print(@NotNull String text) {
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

  private void toggleHandlers(boolean enabled, @NotNull Collection<?> handlers) {
    for (Object handler : handlers) {
      ((ConsoleHandler<?>) handler).setEnabled(enabled);
    }
  }

  @NotNull
  @ApiStatus.Internal
  protected LineReader getLineReader() {
    return this.lineReader;
  }

  @NotNull
  @ApiStatus.Internal
  protected Map<UUID, ConsoleHandler<Consumer<String>>> getConsoleInputHandler() {
    return this.consoleInputHandler;
  }

  @NotNull
  @ApiStatus.Internal
  protected Collection<ITabCompleter> getTabCompletionHandler() {
    return this.tabCompletionHandler.values().stream()
      .filter(ConsoleHandler::isEnabled)
      .map(ConsoleHandler::getHandler)
      .collect(Collectors.toList());
  }

  private final class InternalLineReader extends LineReaderImpl {

    private InternalLineReader(Terminal terminal, String appName, Map<String, Object> variables) {
      super(terminal, appName, variables);
    }

    @Override
    protected boolean historySearchBackward() {
      if (JLine3Console.this.isUsingMatchingHistoryComplete()) {
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
      if (JLine3Console.this.isUsingMatchingHistoryComplete()) {
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

    private InternalLineReaderBuilder(@NotNull Terminal terminal) {
      this.terminal = terminal;
    }

    public @NotNull InternalLineReaderBuilder variable(@NotNull String name, @NotNull Object value) {
      this.variables.put(name, value);
      return this;
    }

    public @NotNull InternalLineReaderBuilder option(@NotNull LineReader.Option option, boolean value) {
      this.options.put(option, value);
      return this;
    }

    public @NotNull InternalLineReaderBuilder completer(@NotNull Completer completer) {
      this.completer = completer;
      return this;
    }

    public @NotNull InternalLineReader build() {
      InternalLineReader reader = new InternalLineReader(this.terminal, "CloudNet-Console", this.variables);
      if (this.completer != null) {
        reader.setCompleter(this.completer);
      }

      for (Map.Entry<LineReader.Option, Boolean> e : this.options.entrySet()) {
        reader.option(e.getKey(), e.getValue());
      }

      return reader;
    }
  }
}
