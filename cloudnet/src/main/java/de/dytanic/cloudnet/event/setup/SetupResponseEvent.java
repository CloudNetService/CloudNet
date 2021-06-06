package de.dytanic.cloudnet.event.setup;

import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;

public class SetupResponseEvent extends SetupEvent {

  private final QuestionListEntry<?> responseEntry;
  private final Object response;

  public SetupResponseEvent(ConsoleQuestionListAnimation setup, QuestionListEntry<?> responseEntry, Object response) {
    super(setup);
    this.responseEntry = responseEntry;
    this.response = response;
  }

  public QuestionListEntry<?> getResponseEntry() {
    return this.responseEntry;
  }

  public Object getResponse() {
    return this.response;
  }
}
