package de.dytanic.cloudnet.console.animation.questionlist.answer;

import com.google.common.net.InetAddresses;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;

public class QuestionAnswerTypeValidHostAndPort implements QuestionAnswerType<HostAndPort> {

    protected final boolean requiresPort;

    public QuestionAnswerTypeValidHostAndPort() {
        this(true);
    }

    public QuestionAnswerTypeValidHostAndPort(boolean requiresPort) {
        this.requiresPort = requiresPort;
    }

    @Override
    public boolean isValidInput(@NotNull String input) {
        return !input.isEmpty() && this.parse(input) != null;
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public @Nullable HostAndPort parse(@NotNull String input) {
        try {
            URI uri = URI.create("tcp://" + input);

            String host = uri.getHost();
            if (host == null || (this.requiresPort && uri.getPort() == -1)) {
                return null;
            }

            InetAddress inetAddress = InetAddresses.forUriString(host);
            return new HostAndPort(inetAddress.getHostAddress(), uri.getPort());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public @Nullable Collection<String> getPossibleAnswers() {
        return null;
    }
}
