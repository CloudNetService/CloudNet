package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import org.fusesource.jansi.AnsiConsole;
import org.jline.reader.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class JLine3Console implements IConsole {

    private final LineReader lineReader;

    private final String user = System.getProperty("user.name");

    private final String version = System.getProperty("cloudnet.launcher.select.version");

    private String prompt = System.getProperty("cloudnet.console.prompt", "&c%user%&r@&7%screen% &f=> &r");

    private String screenName = version;

    private String buffer = null;

    private Map<UUID, AbstractConsoleAnimation> runningAnimations = new ConcurrentHashMap<>();

    private boolean printingEnabled = true;

    private Map<UUID, ConsoleHandler<Consumer<String>>> consoleInputHandler = new ConcurrentHashMap<>();

    private Map<UUID, ConsoleHandler<ITabCompleter>> tabCompletionHandler = new ConcurrentHashMap<>();

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public JLine3Console() throws Exception {
        AnsiConsole.systemInstall();

        Terminal terminal = TerminalBuilder
                .builder()
                .system(true)
                .encoding(StandardCharsets.UTF_8)
                .build();

        this.lineReader = LineReaderBuilder
                .builder()
                .terminal(terminal)
                .completer(new JLine3Completer(this))
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .build();

        this.executorService.execute(() -> {
            while (!Thread.interrupted()) {
                String input = this.call(this.buffer);

                if (input == null) {
                    continue;
                }

                this.buffer = null;

                if (!this.consoleInputHandler.isEmpty()) {
                    for (ConsoleHandler<Consumer<String>> value : this.consoleInputHandler.values()) {
                        if (value.isEnabled()) {
                            value.getHandler().accept(input);
                        }
                    }
                }

                if (!this.runningAnimations.isEmpty()) {
                    for (AbstractConsoleAnimation animation : this.runningAnimations.values()) {
                        animation.addToCursor(1);
                    }
                }
            }
        });
    }

    @Override
    public Collection<AbstractConsoleAnimation> getRunningAnimations() {
        return this.runningAnimations.values();
    }

    @Override
    public void startAnimation(AbstractConsoleAnimation animation) {
        Validate.checkNotNull(animation);

        animation.setConsole(this);

        UUID uniqueId = UUID.randomUUID();
        this.runningAnimations.put(uniqueId, animation);

        this.executorService.execute(() -> {
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
    public List<String> getCommandHistory() {
        List<String> history = new ArrayList<>();
        for (History.Entry entry : this.lineReader.getHistory()) {
            history.add(entry.line());
        }

        return history;
    }

    @Override
    public void setCommandHistory(List<String> history) {
        try {
            this.lineReader.getHistory().purge();
        } catch (final IOException ex) {
            ex.printStackTrace();
        }

        if (history != null) {
            for (String historyEntry : history) {
                this.lineReader.getHistory().add(historyEntry);
            }
        }
    }

    @Override
    @Deprecated
    public void setCommandInputValue(String commandInputValue) {
        this.setBuffer(commandInputValue);
    }

    @Override
    public ITask<String> readLine() {
        Value<String> value = new Value<>();
        ITask<String> task = new ListenableTask<>(value::getValue);

        UUID uniqueId = UUID.randomUUID();
        this.consoleInputHandler.put(uniqueId, new ConsoleHandler<>(input -> {
            this.consoleInputHandler.remove(uniqueId);
            value.setValue(input);
            try {
                task.call();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }));

        return task;
    }

    public Collection<ITabCompleter> getTabCompletionHandler() {
        return this.tabCompletionHandler.values().stream()
                .filter(ConsoleHandler::isEnabled)
                .map(ConsoleHandler::getHandler)
                .collect(Collectors.toList());
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

    private void toggleHandlers(boolean enabled, Collection<?> handlers) {
        for (Object handler : handlers) {
            ((ConsoleHandler<?>) handler).setEnabled(enabled);
        }
    }

    @Override
    public void addCommandHandler(UUID uniqueId, Consumer<String> inputConsumer) {
        this.consoleInputHandler.put(uniqueId, new ConsoleHandler<>(inputConsumer));
    }

    @Override
    public void removeCommandHandler(UUID uniqueId) {
        this.consoleInputHandler.remove(uniqueId);
    }

    @Override
    public void addTabCompletionHandler(UUID uniqueId, ITabCompleter completer) {
        this.tabCompletionHandler.put(uniqueId, new ConsoleHandler<>(completer));
    }

    @Override
    public void removeTabCompletionHandler(UUID uniqueId) {
        this.tabCompletionHandler.remove(uniqueId);
    }

    @Override
    public IConsole write(String text) {
        if (this.printingEnabled) {
            this.forceWrite(text);
        }

        return this;
    }

    @Override
    public IConsole writeLine(String text) {
        if (this.printingEnabled) {
            this.forceWriteLine(text);
        }

        return this;
    }

    @Override
    public IConsole writeDirectly(String text) {
        this.lineReader.getTerminal().puts(InfoCmp.Capability.carriage_return);
        this.lineReader.getTerminal().writer().print(text);
        this.lineReader.getTerminal().writer().flush();

        return this;
    }

    @Override
    public IConsole forceWrite(String text) {
        return this.forceWriteLine(text);
    }

    @Override
    public IConsole writeRaw(String rawText) {
        if (rawText == null) {
            return this;
        }

        rawText = ConsoleColor.toColouredString('&', rawText);

        this.lineReader.getTerminal().puts(InfoCmp.Capability.carriage_return);
        this.lineReader.getTerminal().writer().print(rawText);
        this.lineReader.getTerminal().writer().flush();

        return this;
    }

    @Override
    public IConsole forceWriteLine(String text) {
        if (text == null) {
            return this;
        }

        text = ConsoleColor.toColouredString('&', text);
        text += ConsoleColor.DEFAULT;

        try {
            this.lineReader.getTerminal().puts(InfoCmp.Capability.carriage_return);
            this.lineReader.getTerminal().writer().print(text);
            this.lineReader.getTerminal().writer().flush();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

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
    public String readLine(String prompt, String buffer) throws EndOfFileException, UserInterruptException {
        return this.lineReader.readLine(prompt, null, buffer);
    }

    @Override
    public void resetPrompt() {
        this.prompt = System.getProperty("cloudnet.console.prompt", "&c%user%&r@&7%screen% &f=> &r");
    }

    @Override
    public void clearScreen() {
        this.lineReader.getTerminal().puts(InfoCmp.Capability.clear_screen);
        this.lineReader.getTerminal().flush();
    }

    @Override
    public String getBuffer() {
        return this.buffer;
    }

    @Override
    public void setBuffer(String buffer) {
        this.buffer = buffer;
    }

    @Override
    public void close() {
        try {
            this.executorService.shutdownNow();
            this.lineReader.getTerminal().flush();
            this.lineReader.getTerminal().close();
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    private String call(String buffer) {
        try {
            String prompt = this.prompt != null ? ConsoleColor.toColouredString('&', this.prompt)
                    .replace("%version%", this.version)
                    .replace("%screen%", this.screenName)
                    .replace("%user%", this.user) : null;
            return this.readLine(prompt, buffer);
        } catch (final UserInterruptException ex) {
            System.out.println(LanguageManager.getMessage("cloudnet-user-console-interrupt-warning"));
        } catch (final EndOfFileException ignored) {
        }

        return null;
    }

    public String getUser() {
        return this.user;
    }

    public String getVersion() {
        return this.version;
    }

    public String getPrompt() {
        return this.prompt;
    }

    @Override
    public void reset() {
        if (!this.lineReader.isReading()) {
            return;
        }

        lineReader.callWidget(LineReader.REDRAW_LINE);
        lineReader.callWidget(LineReader.REDISPLAY);

        if (this.buffer != null) {
            this.lineReader.getBuffer().write(this.buffer);
            this.buffer = null;
        }
    }

    @Override
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getScreenName() {
        return this.screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }
}
