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

package de.dytanic.cloudnet.console.animation.questionlist;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import de.dytanic.cloudnet.event.setup.SetupCancelledEvent;
import de.dytanic.cloudnet.event.setup.SetupCompleteEvent;
import de.dytanic.cloudnet.event.setup.SetupInitiateEvent;
import de.dytanic.cloudnet.event.setup.SetupResponseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConsoleQuestionListAnimation extends AbstractConsoleAnimation {

  private final Supplier<String> headerSupplier;
  private final Supplier<String> footerSupplier;
  private final Supplier<Collection<String>> lastCachedMessagesSupplier;

  private final String overwritePrompt;
  private final Map<String, Object> results = new HashMap<>();
  private final Collection<BiConsumer<QuestionListEntry<?>, Object>> entryCompletionListeners = new ArrayList<>();
  private String previousPrompt;
  private boolean previousPrintingEnabled;
  private boolean previousUseMatchingHistorySearch;
  private List<String> previousHistory;
  private Queue<QuestionListEntry<?>> entries = new LinkedBlockingQueue<>();
  private int currentCursor = 1;

  private boolean cancelled = false;
  private boolean cancellable = true;

  public ConsoleQuestionListAnimation(Supplier<Collection<String>> lastCachedMessagesSupplier,
    Supplier<String> headerSupplier, Supplier<String> footerSupplier, String overwritePrompt) {
    this(null, lastCachedMessagesSupplier, headerSupplier, footerSupplier, overwritePrompt);
  }

  public ConsoleQuestionListAnimation(String name, Supplier<Collection<String>> lastCachedMessagesSupplier,
    Supplier<String> headerSupplier, Supplier<String> footerSupplier, String overwritePrompt) {
    super(name);
    this.lastCachedMessagesSupplier = lastCachedMessagesSupplier;
    this.headerSupplier = headerSupplier;
    this.footerSupplier = footerSupplier;
    this.overwritePrompt = overwritePrompt;

    super.setStaticCursor(true);
    super.setCursor(0);
  }

  public void addEntry(@NotNull QuestionListEntry<?> entry) {
    this.entries.add(entry);
  }

  public void addEntriesFirst(@NonNls QuestionListEntry<?>... entries) {
    if (entries.length == 0) {
      return;
    }

    Queue<QuestionListEntry<?>> newEntries = new LinkedBlockingQueue<>(Arrays.asList(entries));
    newEntries.addAll(this.entries);
    this.entries = newEntries;
  }

  public void addEntriesAfter(@NotNull String keyBefore, @NonNls QuestionListEntry<?>... entries) {
    if (entries.length == 0) {
      return;
    }

    Queue<QuestionListEntry<?>> newEntries = new LinkedBlockingQueue<>();
    for (QuestionListEntry<?> oldEntry : this.entries) {
      newEntries.add(oldEntry);
      if (oldEntry.getKey().equals(keyBefore)) {
        newEntries.addAll(Arrays.asList(entries));
      }
    }

    this.entries = newEntries;
  }

  public void addEntriesBefore(@NotNull String keyAfter, @NonNls QuestionListEntry<?>... entries) {
    if (entries.length == 0) {
      return;
    }

    Queue<QuestionListEntry<?>> newEntries = new LinkedBlockingQueue<>();
    for (QuestionListEntry<?> oldEntry : this.entries) {
      if (oldEntry.getKey().equals(keyAfter)) {
        newEntries.addAll(Arrays.asList(entries));
      }

      newEntries.add(oldEntry);
    }

    this.entries = newEntries;
  }

  public boolean isCancelled() {
    return this.cancelled;
  }

  public boolean isCancellable() {
    return this.cancellable;
  }

  public void setCancellable(boolean cancellable) {
    this.cancellable = cancellable;
  }

  public @NotNull Map<String, Object> getResults() {
    return this.results;
  }

  public @Nullable Object getResult(@NotNull String key) {
    return this.results.get(key);
  }

  public boolean hasResult(@NotNull String key) {
    return this.results.containsKey(key);
  }

  public void addEntryCompletionListener(@NotNull BiConsumer<QuestionListEntry<?>, Object> listener) {
    this.entryCompletionListeners.add(listener);
  }

  @Override
  public void setConsole(@NotNull IConsole console) {
    super.setConsole(console);
    this.previousPrintingEnabled = console.isPrintingEnabled();
    this.previousUseMatchingHistorySearch = console.isUsingMatchingHistoryComplete();
    this.previousPrompt = console.getPrompt();
    this.previousHistory = console.getCommandHistory();

    console.setCommandHistory(null);
    console.setUsingMatchingHistoryComplete(false);

    if (this.overwritePrompt != null) {
      console.setPrompt(this.overwritePrompt);
    }

    String header = this.headerSupplier.get();
    if (header != null) {
      console.forceWriteLine(header);
    }

    console.forceWriteLine("&e" + LanguageManager.getMessage("ca-question-list-explain"));
    if (this.isCancellable()) {
      console.forceWriteLine("&e" + LanguageManager.getMessage("ca-question-list-cancel"));
    }

    console.disableAllHandlers();
    CloudNet.getInstance().getEventManager().callEvent(new SetupInitiateEvent(this));
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
        + " &r> &e" + LanguageManager.getMessage("ca-question-list-possible-answers-list")
        .replace("%values%", possibleAnswers))) {
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

  private boolean validateInput(@NotNull QuestionAnswerType<?> answerType, @NotNull QuestionListEntry<?> entry,
    @NotNull String input) {
    if (this.isCancellable() && input.equalsIgnoreCase("cancel")) {
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

      CloudNet.getInstance().getEventManager().callEvent(new SetupResponseEvent(this, entry, result));

      // print result message and remove question
      super.getConsole().writeRaw(
        this.eraseLines(Ansi.ansi().reset(), this.currentCursor + 1)
          .a("&r").a(entry.getQuestion())
          .a(" &r> &a").a(input)
          .a(System.lineSeparator())
          .toString()
      );

      return true;
    }

    try {
      super.eraseLastLine(); // erase prompt

      String[] lines = answerType.getInvalidInputMessage(input).split(System.lineSeparator());
      for (String line : lines) {
        super.getConsole().forceWriteLine("&c" + line);
      }

      Thread.sleep(3000);

      super.getConsole()
        .writeRaw(this.eraseLines(Ansi.ansi().reset(), lines.length).toString()); //erase invalid input message
      super.getConsole().setCommandHistory(answerType.getCompletableAnswers());
    } catch (InterruptedException exception) {
      exception.printStackTrace();
    }

    return false;
  }

  private void setDefaultConsoleValues(@NotNull QuestionAnswerType<?> answerType) {
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
      CloudNet.getInstance().getEventManager().callEvent(new SetupCancelledEvent(this));
    } else {
      String footer = this.footerSupplier.get();
      if (footer != null) {
        super.getConsole().forceWriteLine("&r" + footer);
      }

      CloudNet.getInstance().getEventManager().callEvent(new SetupCompleteEvent(this));
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
    super.getConsole().setUsingMatchingHistoryComplete(this.previousUseMatchingHistorySearch);
    super.getConsole().setCommandHistory(this.previousHistory);
  }

  private String[] updateCursor(@NonNls String... texts) {
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

  private @NotNull Ansi eraseLines(@NotNull Ansi ansi, int count) {
    for (int i = 0; i < count; i++) {
      ansi.cursorUp(1);
      ansi.eraseLine();
    }

    return ansi;
  }
}
