package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.util.PortValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuestionAnswerTypeHostAndPort extends QuestionAnswerTypeValidHostAndPort {

  public QuestionAnswerTypeHostAndPort() {
    super();
  }

  public QuestionAnswerTypeHostAndPort(boolean requiresPort) {
    super(requiresPort);
  }

  @Override
  public @Nullable HostAndPort parse(@NotNull String input) {
    HostAndPort parsedOutput = super.parse(input);
    if (parsedOutput == null) {
      return null;
    }

    boolean valid = this.requiresPort
      ? PortValidator.checkHost(parsedOutput.getHost(), parsedOutput.getPort())
      : PortValidator.canAssignAddress(parsedOutput.getHost());
    return valid ? parsedOutput : null;
  }
}
