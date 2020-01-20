package de.dytanic.cloudnet.event.setup;

import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;

public class SetupCompleteEvent extends SetupEvent {
    public SetupCompleteEvent(ConsoleQuestionListAnimation setup) {
        super(setup);
    }
}
