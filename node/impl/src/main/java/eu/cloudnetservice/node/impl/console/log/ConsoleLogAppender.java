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

package eu.cloudnetservice.node.impl.console.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.node.impl.console.Console;
import lombok.NonNull;

public class ConsoleLogAppender extends ConsoleAppender<ILoggingEvent> {

  private final Console console;

  public ConsoleLogAppender() {
    this.console = InjectionLayer.boot().instance(Console.class);
  }

  @Override
  protected void append(@NonNull ILoggingEvent event) {
    this.console.writeLine(new String(super.encoder.encode(event)));
  }
}
