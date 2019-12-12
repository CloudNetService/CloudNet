package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import de.dytanic.cloudnet.driver.network.HostAndPort;

import java.util.Collection;

public class QuestionAnswerTypeHostAndPort implements QuestionAnswerType<HostAndPort> {
    @Override
    public boolean isValidInput(String input) {
        return !input.isEmpty() && parse(input) != null;
    }

    @Override
    public HostAndPort parse(String input) {
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
        }
        return null;
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return null;
    }
}
