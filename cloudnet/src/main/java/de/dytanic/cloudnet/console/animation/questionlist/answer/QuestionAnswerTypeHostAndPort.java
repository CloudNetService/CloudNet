package de.dytanic.cloudnet.console.animation.questionlist.answer;

import com.google.common.net.InetAddresses;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import de.dytanic.cloudnet.driver.network.HostAndPort;

import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;

public class QuestionAnswerTypeHostAndPort implements QuestionAnswerType<HostAndPort> {

    @Override
    public boolean isValidInput(String input) {
        return !input.isEmpty() && this.parse(input) != null;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public HostAndPort parse(String input) {
        try {
            URI uri = URI.create("tcp://" + input);

            String host = uri.getHost();
            if (host == null) {
                return null;
            }

            InetAddress inetAddress = InetAddresses.forUriString(host);
            return new HostAndPort(inetAddress.getHostAddress(), uri.getPort());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return null;
    }

}
