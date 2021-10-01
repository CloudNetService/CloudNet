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

package de.dytanic.cloudnet.service.defaults;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.IServiceConsoleLogCache;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;

public class JVMService extends AbstractService {

  private static final Logger LOGGER = LogManager.getLogger(JVMService.class);

  protected final IServiceConsoleLogCache logCache;
  protected final Lock lifecycleLock = new ReentrantLock(true);

  protected volatile Process process;

  protected JVMService(@NotNull ServiceConfiguration configuration, @NotNull ICloudServiceManager manager) {
    super(configuration, manager);
  }

  @Override
  public void init() {

  }

  @Override
  public void setCloudServiceLifeCycle(@NotNull ServiceLifeCycle lifeCycle) {
    try {
      // prevent multiple service updates at the same time
      this.lifecycleLock.lock();
    } finally {
      this.lifecycleLock.unlock();
    }
  }

  @Override
  public void runCommand(@NotNull String command) {
    if (this.process != null) {
      try {
        OutputStream out = this.process.getOutputStream();
        // write & flush
        out.write(command.getBytes(StandardCharsets.UTF_8));
        out.flush();
      } catch (IOException exception) {
        LOGGER.severe("Unable to dispatch command %s on service %s", exception, command, this.getServiceId());
      }
    }
  }

  @Override
  public @NotNull String getRuntime() {
    return "jvm";
  }

  @Override
  public @NotNull IServiceConsoleLogCache getServiceConsoleLogCache() {
    return this.logCache;
  }

  @Override
  public void delete(boolean sendUpdate) {

  }

  @Override
  public boolean isAlive() {
    return this.process != null && this.process.isAlive();
  }
}
