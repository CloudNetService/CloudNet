package de.dytanic.cloudnet.setup;

import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;

public interface DefaultSetup {

    void applyQuestions(ConsoleQuestionListAnimation animation) throws Exception;

    default void execute(ConsoleQuestionListAnimation animation) {
    }

    default void postExecute(ConsoleQuestionListAnimation animation) {
    }

    boolean shouldAsk(boolean configFileAvailable);

}
