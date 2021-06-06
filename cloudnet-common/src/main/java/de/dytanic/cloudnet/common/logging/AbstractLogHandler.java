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

package de.dytanic.cloudnet.common.logging;

import org.jetbrains.annotations.NotNull;

/**
 * This is a basic abstract implementation of the ILogHandler class. It should help, to create a simple
 */
public abstract class AbstractLogHandler implements ILogHandler {

  /**
   * A formatter with a default initialization value with the DefaultLogFormatter class.
   *
   * @see DefaultLogFormatter
   */
  protected IFormatter formatter;

  public AbstractLogHandler() {
    this(new DefaultLogFormatter());
  }

  public AbstractLogHandler(@NotNull IFormatter formatter) {
    this.formatter = formatter;
  }

  public @NotNull IFormatter getFormatter() {
    return this.formatter;
  }

  /**
   * Set the new formatter
   *
   * @return the current instance of the AbstractLogHandler class
   */
  public @NotNull AbstractLogHandler setFormatter(@NotNull IFormatter formatter) {
    this.formatter = formatter;
    return this;
  }

  @Override
  public void close() throws Exception {
  }
}
