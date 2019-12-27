package de.dytanic.cloudnet.event.setup;

import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;

public class SetupCancelledEvent extends SetupEvent {
    public SetupCancelledEvent(ConsoleQuestionListAnimation setup) {
        super(setup);
    }
}
