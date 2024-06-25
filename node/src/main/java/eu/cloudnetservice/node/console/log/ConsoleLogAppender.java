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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.node.console.Console;
import lombok.NonNull;

public class ConsoleLogAppender extends AppenderBase<ILoggingEvent> {

  private final Console console;

  private Encoder<ILoggingEvent> encoder;

  public ConsoleLogAppender() {
    this.console = InjectionLayer.boot().instance(Console.class);
  }

  public void setEncoder(@NonNull Encoder<ILoggingEvent> encoder) {
    this.encoder = encoder;
  }

  @Override
  protected void append(ILoggingEvent event) {
    this.console.writeLine(new String(this.encoder.encode(event)));
  }

  @Override
  public void start() {
    if (this.encoder != null) {
      this.encoder.start();
    }

    super.start();
  }

  @Override
  public void stop() {
    if (this.encoder != null) {
      this.encoder.stop();
    }

    super.stop();
  }
}
