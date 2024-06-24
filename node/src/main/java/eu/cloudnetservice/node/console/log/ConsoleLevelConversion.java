/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.console.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import eu.cloudnetservice.node.console.ConsoleColor;
import lombok.NonNull;

public class ConsoleLevelConversion extends CompositeConverter<ILoggingEvent> {

  @Override
  public String transform(@NonNull ILoggingEvent event, @NonNull String input) {
    return this.color(event.getLevel()) + input + ConsoleColor.DEFAULT;
  }

  private @NonNull String color(@NonNull Level level) {
    var color = ConsoleColor.DARK_GRAY;
    if (level == Level.INFO) {
      color = ConsoleColor.GREEN;
    } else if (level == Level.WARN) {
      color = ConsoleColor.YELLOW;
    } else if (level == Level.ERROR) {
      color = ConsoleColor.RED;
    } else if (level.toInt() >= Level.TRACE_INT && level.toInt() <= Level.DEBUG_INT) {
      color = ConsoleColor.BLUE;
    }

    return color.toString();
  }
}
