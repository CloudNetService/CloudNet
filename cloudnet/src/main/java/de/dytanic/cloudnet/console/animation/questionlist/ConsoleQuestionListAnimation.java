package de.dytanic.cloudnet.console.animation.questionlist;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import org.fusesource.jansi.Ansi;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConsoleQuestionListAnimation extends AbstractConsoleAnimation {
    private Supplier<String> headerSupplier, footerSupplier;
    private Supplier<Collection<String>> lastCachedMessagesSupplier;

    private String overwritePrompt;

    private String previousPrompt;
    private boolean previousPrintingEnabled;
    private List<String> previousHistory;

    private Map<String, Object> results = new HashMap<>();

    private Queue<QuestionListEntry<?>> entries = new LinkedBlockingQueue<>();

    private Collection<BiConsumer<QuestionListEntry<?>, Object>> entryCompletionListeners = new ArrayList<>();

    private int currentCursor = 1;

    private boolean cancelled = false;

    public ConsoleQuestionListAnimation(Supplier<Collection<String>> lastCachedMessagesSupplier, Supplier<String> headerSupplier, Supplier<String> footerSupplier, String overwritePrompt) {
        this.lastCachedMessagesSupplier = lastCachedMessagesSupplier;
        this.headerSupplier = headerSupplier;
        this.footerSupplier = footerSupplier;
        this.overwritePrompt = overwritePrompt;

        super.setStaticCursor(true);
        super.setCursor(0);
    }

    public void addEntry(QuestionListEntry<?> entry) {
        this.entries.add(entry);
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public Map<String, Object> getResults() {
        return this.results;
    }

    public Object getResult(String key) {
        return this.results.get(key);
    }

    public boolean hasResult(String key) {
        return this.results.containsKey(key);
    }

    public void addEntryCompletionListener(BiConsumer<QuestionListEntry<?>, Object> listener) {
        this.entryCompletionListeners.add(listener);
    }

    @Override
    public void setConsole(IConsole console) {
        super.setConsole(console);
        this.previousPrintingEnabled = console.isPrintingEnabled();
        this.previousPrompt = console.getPrompt();
        this.previousHistory = console.getCommandHistory();

        console.setCommandHistory(null);

        if (this.overwritePrompt != null) {
            console.setPrompt(this.overwritePrompt);
        }

        String header = this.headerSupplier.get();
        if (header != null) {
            console.forceWriteLine(header);
        }

        console.forceWriteLine("&e" + LanguageManager.getMessage("ca-question-list-explain"));
        console.forceWriteLine("&e" + LanguageManager.getMessage("ca-question-list-cancel"));

        console.disableAllHandlers();

    }

    @Override
    protected boolean handleTick() {
        QuestionListEntry<?> entry;

        if (this.entries.isEmpty() || (entry = this.entries.poll()) == null) {
            return true;
        }

        QuestionAnswerType<?> answerType = entry.getAnswerType();

        this.setDefaultConsoleValues(answerType);

        String possibleAnswers = answerType.getPossibleAnswersAsString();
        if (possibleAnswers != null) {
            for (String line : this.updateCursor("&r" + entry.getQuestion()
                    + " &r> &e" + LanguageManager.getMessage("ca-question-list-possible-answers-list").replace("%values%", possibleAnswers))) {
                super.getConsole().forceWriteLine("&e" + line);
            }
        } else {
            for (String line : this.updateCursor("&r" + entry.getQuestion())) {
                super.getConsole().forceWriteLine(line);
            }
        }


        ITask<Void> task = new ListenableTask<>(() -> null);
        UUID handlerId = UUID.randomUUID();

        super.getConsole().addCommandHandler(handlerId, input -> {
            if (this.validateInput(answerType, entry, input)) {
                if (this.entries.isEmpty()) {
                    this.resetConsole();
                }

                super.getConsole().removeCommandHandler(handlerId);
                try {
                    task.call();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });

        try {
            task.get();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }

    private boolean validateInput(QuestionAnswerType<?> answerType, QuestionListEntry<?> entry, String input) {
        if (input.equalsIgnoreCase("cancel")) {
            this.cancelled = true;
            this.entries.clear();

            return true;
        }

        if (answerType.isValidInput(input)) {
            Object result = answerType.parse(input);
            this.results.put(entry.getKey(), result);
            for (BiConsumer<QuestionListEntry<?>, Object> listener : this.entryCompletionListeners) {
                listener.accept(entry, result);
            }
            super.getConsole().writeRaw( //print result message and remove question
                    this.eraseLines(
                            Ansi.ansi()
                                    .reset(),
                            this.currentCursor + 1)
                            .a("&r").a(entry.getQuestion())
                            .a(" &r> &a").a(input)
                            .a(System.lineSeparator())
                            .toString()
            );

            return true;
        }

        try {
            super.eraseLastLine(); //erase prompt
            super.getConsole().forceWriteLine("&c" + answerType.getInvalidInputMessage(input));
            Thread.sleep(3000);
            super.eraseLastLine(); //erase invalid input message
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    private void setDefaultConsoleValues(QuestionAnswerType<?> answerType) {
        super.getConsole().setCommandHistory(answerType.getCompletableAnswers());

        String recommendation = answerType.getRecommendation();
        if (recommendation != null) {
            super.getConsole().setCommandInputValue(recommendation);
        }

        super.getConsole().togglePrinting(false);
    }

    private void resetConsole() {
        if (this.cancelled) {
            super.getConsole().forceWriteLine("&c" + LanguageManager.getMessage("ca-question-list-cancelled"));
        } else {
            String footer = this.footerSupplier.get();

            if (footer != null) {
                super.getConsole().forceWriteLine("&r" + footer);
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }

        super.getConsole().clearScreen();
        if (this.lastCachedMessagesSupplier != null) {
            for (String line : this.lastCachedMessagesSupplier.get()) {
                super.getConsole().forceWriteLine(line);
            }
        }

        super.getConsole().enableAllHandlers();
        super.getConsole().togglePrinting(this.previousPrintingEnabled);
        super.getConsole().setPrompt(this.previousPrompt);
        super.getConsole().setCommandHistory(this.previousHistory);
    }

    private String[] updateCursor(String... texts) {
        Collection<String> result = new ArrayList<>(texts.length);
        int length = 0;
        for (String text : texts) {
            for (String line : text.split(System.lineSeparator())) {
                ++length;
                result.add(line);
            }
        }
        this.currentCursor = length;
        return result.toArray(new String[0]);
    }

    private Ansi eraseLines(Ansi ansi, int count) {
        for (int i = 0; i < count; i++) {
            ansi.cursorUp(1);
            ansi.eraseLine();
        }
        return ansi;
    }

}
