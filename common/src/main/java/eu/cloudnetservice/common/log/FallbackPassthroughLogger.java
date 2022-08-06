/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.common.log;

import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A logger instance which wraps a given java.util.logging logger.
 *
 * @since 4.0
 */
@ApiStatus.Internal
final class FallbackPassthroughLogger extends Logger {

  private final java.util.logging.Logger logger;
  private LogRecordDispatcher logRecordDispatcher;

  /**
   * Constructs a new fallback logger instance based on the given java.util.logging logger.
   *
   * @param logger the logger to wrap.
   * @throws NullPointerException if the given logger to wrap is null.
   */
  FallbackPassthroughLogger(@NonNull java.util.logging.Logger logger) {
    super(logger.getName(), logger.getResourceBundleName());
    this.logger = logger;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void log(LogRecord record) {
    if (this.logRecordDispatcher == null) {
      this.logger.log(record);
    } else {
      this.logRecordDispatcher.dispatchRecord(this, record);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void forceLog(@NonNull LogRecord logRecord) {
    this.logger.log(logRecord);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable LogRecordDispatcher logRecordDispatcher() {
    return this.logRecordDispatcher;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logRecordDispatcher(@Nullable LogRecordDispatcher dispatcher) {
    this.logRecordDispatcher = dispatcher;
  }

  // passthrough methods

  /**
   * {@inheritDoc}
   */
  @Override
  public ResourceBundle getResourceBundle() {
    return this.logger.getResourceBundle();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setResourceBundle(ResourceBundle bundle) {
    this.logger.setResourceBundle(bundle);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getResourceBundleName() {
    return this.logger.getResourceBundleName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Filter getFilter() {
    return this.logger.getFilter();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFilter(Filter newFilter) throws SecurityException {
    this.logger.setFilter(newFilter);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Level getLevel() {
    var thisLevel = this.logger.getLevel();
    return thisLevel == null ? LogManager.rootLogger().getLevel() : thisLevel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLevel(Level newLevel) throws SecurityException {
    this.logger.setLevel(newLevel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLoggable(Level level) {
    return this.logger.isLoggable(level);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addHandler(Handler handler) throws SecurityException {
    this.logger.addHandler(handler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeHandler(Handler handler) throws SecurityException {
    this.logger.removeHandler(handler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Handler[] getHandlers() {
    return this.logger.getHandlers();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getUseParentHandlers() {
    return this.logger.getUseParentHandlers();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setUseParentHandlers(boolean useParentHandlers) {
    this.logger.setUseParentHandlers(useParentHandlers);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public java.util.logging.Logger getParent() {
    return this.logger.getParent();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setParent(java.util.logging.Logger parent) {
    this.logger.setParent(parent);
  }
}
