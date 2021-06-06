package de.dytanic.cloudnet.event.setup;

import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.driver.event.Event;

public class SetupEvent extends Event {

  private final ConsoleQuestionListAnimation setup;

  public SetupEvent(ConsoleQuestionListAnimation setup) {
    this.setup = setup;
  }

  public ConsoleQuestionListAnimation getSetup() {
    return this.setup;
  }
}
