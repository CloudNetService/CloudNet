package de.dytanic.cloudnet.event.setup;

import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;

public class SetupInitiateEvent extends SetupEvent {

  public SetupInitiateEvent(ConsoleQuestionListAnimation setup) {
    super(setup);
  }
}
