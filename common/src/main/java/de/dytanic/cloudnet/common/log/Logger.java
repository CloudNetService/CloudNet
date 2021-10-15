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

package de.dytanic.cloudnet.common.log;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Logger extends java.util.logging.Logger {

  protected Logger(String name, String resourceBundleName) {
    super(name, resourceBundleName);
  }

  public abstract void forceLog(@NotNull LogRecord logRecord);

  public abstract @Nullable LogRecordDispatcher getLogRecordDispatcher();

  public abstract void setLogRecordDispatcher(@Nullable LogRecordDispatcher dispatcher);

  public void fine(@NotNull String message, @NotNull Throwable throwable, Object @NotNull ... params) {
    this.log(Level.FINE, this.doFormat(message, params), throwable);
  }

  public void finer(@NotNull String message, @NotNull Throwable throwable, Object @NotNull ... params) {
    this.log(Level.FINER, this.doFormat(message, params), throwable);
  }

  public void finest(@NotNull String message, @NotNull Throwable throwable, Object @NotNull ... params) {
    this.log(Level.FINEST, this.doFormat(message, params), throwable);
  }

  public void severe(@NotNull String message, @NotNull Throwable throwable, Object @NotNull ... params) {
    this.log(Level.SEVERE, this.doFormat(message, params), throwable);
  }

  public void warning(@NotNull String message, @NotNull Throwable throwable, Object @NotNull ... params) {
    this.log(Level.WARNING, this.doFormat(message, params), throwable);
  }

  public void info(@NotNull String message, @NotNull Throwable throwable, Object @NotNull ... params) {
    this.log(Level.INFO, this.doFormat(message, params), throwable);
  }

  public void config(@NotNull String message, @NotNull Throwable throwable, Object @NotNull ... params) {
    this.log(Level.CONFIG, this.doFormat(message, params), throwable);
  }

  protected @NotNull String doFormat(@NotNull String message, Object @NotNull ... replacements) {
    return replacements.length == 0 ? message : String.format(message, replacements);
  }
}
