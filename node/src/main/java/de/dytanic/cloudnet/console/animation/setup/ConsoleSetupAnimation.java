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

package de.dytanic.cloudnet.console.animation.setup;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.console.Console;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionAnswerType;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionListEntry;
import de.dytanic.cloudnet.console.handler.ConsoleInputHandler;
import de.dytanic.cloudnet.event.setup.SetupCancelledEvent;
import de.dytanic.cloudnet.event.setup.SetupCompleteEvent;
import de.dytanic.cloudnet.event.setup.SetupInitiateEvent;
import de.dytanic.cloudnet.event.setup.SetupResponseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import lombok.NonNull;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.Nullable;

public class ConsoleSetupAnimation extends AbstractConsoleAnimation {

  // session values
  private final Object monitor;
  private final UUID handlerId;

  // style settings
  private final String header;
  private final String footer;
  private final String overwritePrompt;

  private final Map<String, Object> results = new HashMap<>();
  private final Deque<QuestionListEntry<?>> entries = new LinkedBlockingDeque<>();

  // old console settings
  private String previousPrompt;
  private boolean previousPrintingEnabled;
  private Collection<String> previousHistory;
  private Collection<String> previousConsoleLines;
  private boolean previousUseMatchingHistorySearch;

  // cursor position for erase calls
  private int currentCursor = 1;

  // cancel settings
  private boolean cancelled = false;
  private boolean cancellable = true;

  public ConsoleSetupAnimation(@Nullable String header, @Nullable String footer, @Nullable String overwritePrompt) {
    super(25);

    // session values
    this.monitor = new Object();
    this.handlerId = UUID.randomUUID();

    // style settings
    this.header = header;
    this.footer = footer;
    this.overwritePrompt = overwritePrompt;

    // animation settings
    this.cursorUp = 0;
    this.staticCursor = true;
  }

  public void addEntries(QuestionListEntry<?> @NonNull ... entries) {
    if (entries.length != 0) {
      for (var entry : entries) {
        this.entries.offerLast(entry);
      }
    }
  }

  public void addEntriesFirst(QuestionListEntry<?> @NonNull ... entries) {
    if (entries.length != 0) {
      for (var i = entries.length - 1; i >= 0; i--) {
        this.entries.offerFirst(entries[i]);
      }
    }
  }

  public @NonNull Deque<QuestionListEntry<?>> entries() {
    return this.entries;
  }

  public boolean cancelled() {
    return this.cancelled;
  }

  public boolean cancellable() {
    return this.cancellable;
  }

  public void cancellable(boolean cancellable) {
    this.cancellable = cancellable;
  }

  @SuppressWarnings("unchecked")
  public <T> T result(@NonNull String key) {
    return (T) this.results.get(key);
  }

  public boolean hasResult(@NonNull String key) {
    return this.results.containsKey(key);
  }

  @Override
  public void console(@NonNull Console console) {
    super.console(console);

    // store the current console setting for resetting later
    this.previousPrompt = console.prompt();
    this.previousHistory = console.commandHistory();
    this.previousPrintingEnabled = console.printingEnabled();
    this.previousUseMatchingHistorySearch = console.usingMatchingHistoryComplete();
    this.previousConsoleLines = CloudNet.instance().logHandler().formattedCachedLogLines();

    // apply the console settings of the animation
    console.clearScreen();
    console.commandHistory(null);
    console.togglePrinting(false);
    console.usingMatchingHistoryComplete(false);

    if (this.overwritePrompt != null) {
      console.prompt(this.overwritePrompt);
    }

    // print the header if supplied
    if (this.header != null) {
      console.forceWriteLine(this.header);
    }

    // print a general explanation of the setup process
    console.forceWriteLine("&e" + I18n.trans("ca-question-list-explain"));
    if (this.cancellable()) {
      console.forceWriteLine("&e" + I18n.trans("ca-question-list-cancel"));
    }

    // disable all commands of the console
    console.disableAllHandlers();
    CloudNet.instance().eventManager().callEvent(new SetupInitiateEvent(this));
  }

  @Override
  protected boolean handleTick() {
    // check if there are more entries to go
    var entry = this.entries.poll();
    if (entry == null) {
      // no more questions - stop the animation
      return true;
    }

    var answerType = entry.answerType();
    // write the recommendation if given
    if (answerType.recommendation() != null) {
      this.console.commandInputValue(answerType.recommendation());
    }
    // check for possible answers
    if (!answerType.possibleAnswers().isEmpty()) {
      // register the tab complete
      this.console.addTabCompleteHandler(
        this.handlerId,
        new ConsoleAnswerTabCompleteHandler(answerType.possibleAnswers()));
      // set the answers in the console history
      this.console.commandHistory(answerType.possibleAnswers());

      // collect the possible answers to one string
      var answers = I18n.trans("ca-question-list-possible-answers-list")
        .replace("%values%", String.join(", ", answerType.possibleAnswers()));
      // write the answers to the console
      for (var line : this.updateCursor("&r" + entry.question() + " &r> &e" + answers)) {
        super.console().forceWriteLine(line);
      }
    } else {
      // clear the history
      this.console.commandHistory(null);
      // just write the question into the console
      for (var line : this.updateCursor("&r" + entry.question())) {
        super.console().forceWriteLine(line);
      }
    }

    // add the command handler which handles the input
    this.console.addCommandHandler(this.handlerId, new ConsoleInputHandler() {
      @Override
      public void handleInput(@NonNull String line) {
        try {
          // check if the input was handled - wait for the response if not
          if (ConsoleSetupAnimation.this.handleInput(answerType, entry, line)) {
            // remove the handlers
            ConsoleSetupAnimation.this.console.removeCommandHandler(ConsoleSetupAnimation.this.handlerId);
            ConsoleSetupAnimation.this.console.removeTabCompleteHandler(ConsoleSetupAnimation.this.handlerId);
            // notify the monitor
            synchronized (ConsoleSetupAnimation.this.monitor) {
              ConsoleSetupAnimation.this.monitor.notifyAll();
            }
          }
        } catch (InterruptedException exception) {
          throw new RuntimeException("Console thread got interrupted during handling of response input", exception);
        }
      }
    });

    try {
      synchronized (this.monitor) {
        this.monitor.wait();
      }
    } catch (InterruptedException exception) {
      // interrupt the thread execution
      throw new RuntimeException("Exception during wait for monitor notify", exception);
    }

    return false;
  }

  @Override
  public void handleDone() {
    if (!this.cancelled) {
      super.handleDone();
    }
  }

  protected boolean handleInput(
    @NonNull QuestionAnswerType<?> answerType,
    @NonNull QuestionListEntry<?> entry,
    @NonNull String input
  ) throws InterruptedException {
    // check if the setup was cancelled using the input
    if (input.equalsIgnoreCase("cancel") && ConsoleSetupAnimation.this.cancellable) {
      this.cancelled = true;
      this.entries.clear();
      return true;
    }

    // try to parse the input from the type
    try {
      var result = answerType.tryParse(input.trim());
      // store the result and post it to the handlers
      answerType.postResult(result);
      this.results.put(entry.key(), result);
      // call the event
      CloudNet.instance().eventManager().callEvent(new SetupResponseEvent(this, entry, result));
      // re-draw the question line, add the given response to it
      this.console.writeRaw(this.eraseLines(Ansi.ansi().reset(), this.currentCursor + 1)
        .a("&r") // reset of the colors
        .a(entry.question()) // the question
        .a(" &r=> &a") // separator between the question and the answer
        .a(input) // the given result
        .a(System.lineSeparator()) // jump to next line
        .toString());
      // successful handle
      return true;
    } catch (Exception exception) {
      // invalid input
      this.eraseLastLine(); // remove prompt
      // print the invalid input message to the console
      var messageLines = answerType.invalidInputMessage(input).split(System.lineSeparator());
      for (var line : messageLines) {
        this.console.forceWriteLine(line);
      }
      // wait a short period of time for the user to read
      Thread.sleep(1500);
      // erase the invalid lines again
      this.console().writeRaw(this.eraseLines(Ansi.ansi().reset(), messageLines.length).toString());
      // reset the console history
      this.console().commandHistory(answerType.possibleAnswers());
      // continue with the current question
      return false;
    }
  }

  @Override
  public void resetConsole() {
    if (this.cancelled) {
      super.console().forceWriteLine("&c" + I18n.trans("ca-question-list-cancelled"));
      CloudNet.instance().eventManager().callEvent(new SetupCancelledEvent(this));
      // reset the cancelled state
      this.cancelled = false;
    } else {
      // print the footer if supplied
      if (this.footer != null) {
        super.console().forceWriteLine("&r" + this.footer);
      }

      CloudNet.instance().eventManager().callEvent(new SetupCompleteEvent(this));
    }

    try {
      Thread.sleep(1000);
    } catch (InterruptedException exception) {
      LOGGER.severe("Exception while resetting console", exception);
    }

    // remove the setup from the screen
    this.console().clearScreen();

    // write the old lines back if there are some
    if (this.previousConsoleLines.isEmpty()) {
      // send an empty line to prevent bugs
      this.console().forceWriteLine("");
    } else {
      for (var line : this.previousConsoleLines) {
        this.console().forceWriteLine(line);
      }
    }

    // reset the console settings
    this.console().enableAllHandlers();
    this.console().prompt(this.previousPrompt);
    this.console().commandHistory(this.previousHistory);
    this.console().togglePrinting(this.previousPrintingEnabled);
    this.console().usingMatchingHistoryComplete(this.previousUseMatchingHistorySearch);

    super.resetConsole();
  }

  private String @NonNull [] updateCursor(String @NonNull ... texts) {
    Collection<String> result = new ArrayList<>(texts.length);
    var length = 0;
    for (var text : texts) {
      for (var line : text.split(System.lineSeparator())) {
        ++length;
        result.add(line);
      }
    }

    this.currentCursor = length;
    return result.toArray(new String[0]);
  }

  private @NonNull Ansi eraseLines(@NonNull Ansi ansi, int count) {
    for (var i = 0; i < count; i++) {
      ansi.cursorUp(1).eraseLine();
    }

    return ansi;
  }
}
