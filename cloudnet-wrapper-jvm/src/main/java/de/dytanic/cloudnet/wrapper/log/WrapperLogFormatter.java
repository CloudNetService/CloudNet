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

package de.dytanic.cloudnet.wrapper.log;

import de.dytanic.cloudnet.common.logging.IFormatter;
import de.dytanic.cloudnet.common.logging.LogEntry;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.jetbrains.annotations.NotNull;

public final class WrapperLogFormatter implements IFormatter {

  @Override
  public @NotNull String format(@NotNull LogEntry logEntry) {
    StringBuilder builder = new StringBuilder();
    if (logEntry.getThrowable() != null) {
      StringWriter writer = new StringWriter();
      logEntry.getThrowable().printStackTrace(new PrintWriter(writer));
      builder.append(writer).append(System.lineSeparator());
    }

    StringBuilder stringBuilder = new StringBuilder();

    for (String line : logEntry.getMessages()) {
      if (line != null) {
        stringBuilder.append(line);
      }
    }

    return stringBuilder.append(builder).toString();
  }
}
