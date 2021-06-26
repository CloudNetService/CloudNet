/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
