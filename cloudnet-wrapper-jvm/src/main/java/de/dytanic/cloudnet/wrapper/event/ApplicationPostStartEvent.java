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

package de.dytanic.cloudnet.wrapper.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.wrapper.Wrapper;

/**
 * This event is only interesting for wrapper modules. It will called if the app is successful started and will called
 * parallel to the application thread.
 *
 * @see DriverEvent
 */
public final class ApplicationPostStartEvent extends DriverEvent {

  /**
   * The current singleton instance of the Wrapper class
   *
   * @see Wrapper
   */
  private final Wrapper cloudNetWrapper;

  /**
   * The class, which is set in the manifest as 'Main-Class' by the archive of the wrapped application
   */
  private final Class<?> clazz;

  /**
   * The application thread, which invoked the main() method of the Main-Class from the application
   */
  private final Thread applicationThread;

  /**
   * The used ClassLoader
   */
  private final ClassLoader classLoader;

  public ApplicationPostStartEvent(Wrapper cloudNetWrapper, Class<?> clazz, Thread applicationThread,
    ClassLoader classLoader) {
    this.cloudNetWrapper = cloudNetWrapper;
    this.clazz = clazz;
    this.applicationThread = applicationThread;
    this.classLoader = classLoader;
  }

  public Wrapper getCloudNetWrapper() {
    return this.cloudNetWrapper;
  }

  public Class<?> getClazz() {
    return this.clazz;
  }

  public Thread getApplicationThread() {
    return this.applicationThread;
  }

  public ClassLoader getClassLoader() {
    return this.classLoader;
  }
}
