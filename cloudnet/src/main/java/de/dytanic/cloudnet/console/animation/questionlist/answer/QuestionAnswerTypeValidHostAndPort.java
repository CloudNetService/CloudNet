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

import com.google.common.net.InetAddresses;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
