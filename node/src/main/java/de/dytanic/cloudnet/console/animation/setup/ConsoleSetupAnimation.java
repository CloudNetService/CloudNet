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

package de.dytanic.cloudnet.console.animation.setup;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.IConsole;
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
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
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

  public void addEntry(@NotNull QuestionListEntry<?> entry) {
    this.entries.add(entry);
  }

  public void addEntriesFirst(QuestionListEntry<?> @NotNull ... entries) {
    if (entries.length != 0) {
      for (int i = entries.length - 1; i >= 0; i--) {
        this.entries.offerFirst(entries[i]);
      }
    }
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

  @SuppressWarnings("unchecked")
  public <T> T getResult(@NotNull String key) {
    return (T) this.results.get(key);
  }

  public boolean hasResult(@NotNull String key) {
    return this.results.containsKey(key);
  }

  @Override
  public void setConsole(@NotNull IConsole console) {
    super.setConsole(console);

    // store the current console setting for resetting later
    this.previousPrompt = console.getPrompt();
    this.previousHistory = console.getCommandHistory();
    this.previousPrintingEnabled = console.isPrintingEnabled();
    this.previousUseMatchingHistorySearch = console.isUsingMatchingHistoryComplete();
    this.previousConsoleLines = CloudNet.getInstance().getLogHandler().getFormattedCachedLogLines();

    // apply the console settings of the animation
    console.setCommandHistory(null);
    console.togglePrinting(false);
    console.setUsingMatchingHistoryComplete(false);

    if (this.overwritePrompt != null) {
      console.setPrompt(this.overwritePrompt);
    }

    // print the header if supplied
    if (this.header != null) {
      console.forceWriteLine(this.header);
    }

    // print a general explanation of the setup process
    console.forceWriteLine("&e" + LanguageManager.getMessage("ca-question-list-explain"));
    if (this.isCancellable()) {
      console.forceWriteLine("&e" + LanguageManager.getMessage("ca-question-list-cancel"));
    }

    // disable all commands of the console
    console.disableAllHandlers();
    CloudNet.getInstance().getEventManager().callEvent(new SetupInitiateEvent(this));
  }

  @Override
  protected boolean handleTick() {
    // check if there are more entries to go
    QuestionListEntry<?> entry = this.entries.poll();
    if (entry == null) {
      // no more questions - reset the console
      this.resetConsole();
      return true;
    }

    QuestionAnswerType<?> answerType = entry.getAnswerType();
    // write the recommendation if given
    if (answerType.getRecommendation() != null) {
      this.console.setCommandInputValue(answerType.getRecommendation());
    }
    // check for possible answers
    if (!answerType.getPossibleAnswers().isEmpty()) {
      // register the tab complete
      this.console.addTabCompleteHandler(
        this.handlerId,
        new ConsoleAnswerTabCompleteHandler(answerType.getPossibleAnswers()));
      // set the answers in the console history
      this.console.setCommandHistory(answerType.getPossibleAnswers());

      // collect the possible answers to one string
      String answers = LanguageManager.getMessage("ca-question-list-possible-answers-list")
        .replace("%values%", String.join(", ", answerType.getPossibleAnswers()));
      // write the answers to the console
      for (String line : this.updateCursor("&r" + entry.getQuestion() + " &r> &e" + answers)) {
        super.getConsole().forceWriteLine(line);
      }
    } else {
      // clear the history
      this.console.setCommandHistory(null);
      // just write the question into the console
      for (String line : this.updateCursor("&r" + entry.getQuestion())) {
        super.getConsole().forceWriteLine(line);
      }
    }

    // add the command handler which handles the input
    this.console.addCommandHandler(this.handlerId, new ConsoleInputHandler() {
      @Override
      public void handleInput(@NotNull String line) {
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

  protected boolean handleInput(
    @NotNull QuestionAnswerType<?> answerType,
    @NotNull QuestionListEntry<?> entry,
    @NotNull String input
  ) throws InterruptedException {
    // check if the setup was cancelled using the input
    if (input.equalsIgnoreCase("cancel") && ConsoleSetupAnimation.this.cancellable) {
      this.cancelled = true;
      this.entries.clear();
      return true;
    }

    // try to parse the input from the type
    try {
      Object result = answerType.tryParse(input.trim());
      // store the result and post it to the handlers
      answerType.postResult(result);
      this.results.put(entry.getKey(), result);
      // call the event
      CloudNet.getInstance().getEventManager().callEvent(new SetupResponseEvent(this, entry, result));
      // re-draw the question line, add the given response to it
      this.console.writeRaw(this.eraseLines(Ansi.ansi().reset(), this.currentCursor + 1)
        .a("&r") // reset of the colors
        .a(entry.getQuestion()) // the question
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
      String[] messageLines = answerType.getInvalidInputMessage(input).split(System.lineSeparator());
      for (String line : messageLines) {
        this.console.forceWriteLine(line);
      }
      // wait a short period of time for the user to read
      Thread.sleep(1500);
      // erase the invalid lines again
      this.getConsole().writeRaw(this.eraseLines(Ansi.ansi().reset(), messageLines.length).toString());
      // reset the console history
      this.getConsole().setCommandHistory(answerType.getPossibleAnswers());
      // continue with the current question
      return false;
    }
  }

  private void resetConsole() {
    if (this.cancelled) {
      super.getConsole().forceWriteLine("&c" + LanguageManager.getMessage("ca-question-list-cancelled"));
      CloudNet.getInstance().getEventManager().callEvent(new SetupCancelledEvent(this));
    } else {
      // print the footer if supplied
      if (this.footer != null) {
        super.getConsole().forceWriteLine("&r" + this.footer);
      }

      CloudNet.getInstance().getEventManager().callEvent(new SetupCompleteEvent(this));
    }

    try {
      Thread.sleep(1000);
    } catch (InterruptedException exception) {
      LOGGER.severe("Exception while resetting console", exception);
    }

    // write the old console lines back to the screen
    this.getConsole().clearScreen();
    for (String line : this.previousConsoleLines) {
      this.getConsole().forceWriteLine(line);
    }

    // reset the console settings
    this.getConsole().enableAllHandlers();
    this.getConsole().setPrompt(this.previousPrompt);
    this.getConsole().setCommandHistory(this.previousHistory);
    this.getConsole().togglePrinting(this.previousPrintingEnabled);
    this.getConsole().setUsingMatchingHistoryComplete(this.previousUseMatchingHistorySearch);
  }

  private String @NotNull [] updateCursor(String @NotNull ... texts) {
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
      ansi.cursorUp(1).eraseLine();
    }

    return ansi;
  }
}
