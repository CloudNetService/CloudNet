package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class QuestionAnswerTypeHostAndPort implements QuestionAnswerType<HostAndPort> {

    @Override
    public boolean isValidInput(@NotNull String input) {
        return !input.isEmpty() && this.parse(input) != null;
    }

    @Override
    public @NotNull HostAndPort parse(@NotNull String input) {
        String[] splitHostAndPort = input.split(":");
        if (splitHostAndPort.length != 2) {
            return null;
        }
        if (splitHostAndPort[0].split("\\.").length != 4) {
            return null;
        }
        try {
            return new HostAndPort(
                    splitHostAndPort[0],
                    Integer.parseInt(splitHostAndPort[1])
            );
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return null;
    }

}
