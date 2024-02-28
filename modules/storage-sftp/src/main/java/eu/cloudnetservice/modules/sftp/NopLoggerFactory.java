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

package eu.cloudnetservice.modules.sftp;

import net.schmizz.sshj.common.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

final class NopLoggerFactory implements LoggerFactory {

  public static final LoggerFactory INSTANCE = new NopLoggerFactory();

  private NopLoggerFactory() {
  }

  @Override
  public Logger getLogger(String name) {
    return NOPLogger.NOP_LOGGER;
  }

  @Override
  public Logger getLogger(Class<?> clazz) {
    return NOPLogger.NOP_LOGGER;
  }
}
