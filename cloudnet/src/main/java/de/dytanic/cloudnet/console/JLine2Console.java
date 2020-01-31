package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import jline.console.ConsoleReader;
import jline.console.history.History;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class JLine2Console implements IConsole {

    private final ConsoleReader consoleReader;

    private final String
            user = System.getProperty("user.name"),
            version = System.getProperty("cloudnet.launcher.select.version");
    private String prompt = System.getProperty("cloudnet.console.prompt", "&c%user%&r@&7%screen% &f=> &r");

    private String screenName = version;

    private Map<UUID, AbstractConsoleAnimation> runningAnimations = new ConcurrentHashMap<>();

    private boolean printingEnabled = true;

    private Map<UUID, ConsoleHandler<Consumer<String>>> consoleInputHandler = new ConcurrentHashMap<>();
    private Map<UUID, ConsoleHandler<ITabCompleter>> tabCompletionHandler = new ConcurrentHashMap<>();

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public JLine2Console() throws Exception {
        AnsiConsole.systemInstall();

        this.consoleReader = new ConsoleReader();
        this.consoleReader.setExpandEvents(false);

        this.consoleReader.addCompleter(new JLine2Completer(this));

        this.executorService.execute(() -> {
            while (!Thread.interrupted()) {
                try {
                    String input = this.prompt != null ?
                            this.consoleReader.readLine(
                                    ConsoleColor.toColouredString('&', this.prompt)
                                            .replace("%version%", this.version)
                                            .replace("%screen%", this.screenName)
                                            .replace("%user%", this.user)
                            ) : this.consoleReader.readLine();
                    this.resetPrompt();
                    if (input == null) {
                        continue;
                    }

                    if (!this.consoleInputHandler.isEmpty()) {
                        for (ConsoleHandler<Consumer<String>> value : this.consoleInputHandler.values()) {
                            if (value.isEnabled()) {
                                value.getHandler().accept(input);
                            }
                        }
                    }

                } catch (IOException exception) {
                    exception.printStackTrace();
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
        ListIterator<History.Entry> iterator = this.consoleReader.getHistory().entries();
        List<String> history = new ArrayList<>();
        while (iterator.hasNext()) {
            history.add(iterator.next().value().toString());
        }
        return history;
    }

    @Override
    public void setCommandHistory(List<String> history) {
        this.consoleReader.getHistory().clear();
        if (history != null) {
            for (String historyEntry : history) {
                this.consoleReader.getHistory().add(historyEntry);
            }
        }
    }

    @Override
    public void setCommandInputValue(String commandInputValue) {
        try {
            this.consoleReader.putString(commandInputValue);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
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
    public IConsole forceWrite(String text) {
        return this.writeRaw(Ansi.ansi().eraseLine(Ansi.Erase.ALL).toString() + ConsoleReader.RESET_LINE + text + ConsoleColor.DEFAULT);
    }

    @Override
    public IConsole writeRaw(String rawText) {
        if (rawText == null) {
            return this;
        }

        rawText = ConsoleColor.toColouredString('&', rawText);

        try {
            this.consoleReader.print(rawText);
            this.consoleReader.flush();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return this;
    }

    @Override
    public IConsole forceWriteLine(String text) {
        if (text == null) {
            return this;
        }

        text = ConsoleColor.toColouredString('&', text);

        if (!text.endsWith(System.lineSeparator())) {
            text = text + System.lineSeparator();
        }

        try {
            this.consoleReader.print(Ansi.ansi().eraseLine(Ansi.Erase.ALL).toString() + ConsoleReader.RESET_LINE + text + ConsoleColor.DEFAULT);
            this.consoleReader.drawLine();
            this.consoleReader.flush();
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
        return !(this.consoleReader.getTerminal() instanceof jline.UnsupportedTerminal);
    }

    @Override
    public void resetPrompt() {
        this.consoleReader.setPrompt(ConsoleColor.DEFAULT.toString());
    }

    @Override
    public void clearScreen() {
        try {
            this.consoleReader.clearScreen();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void close() {
        this.executorService.shutdownNow();
        this.consoleReader.close();
    }

    public ConsoleReader getConsoleReader() {
        return this.consoleReader;
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
