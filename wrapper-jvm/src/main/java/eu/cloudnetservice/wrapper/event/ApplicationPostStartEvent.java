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

package eu.cloudnetservice.wrapper.event;

import eu.cloudnetservice.driver.event.Event;
import lombok.NonNull;

/**
 * An event called when the underlying application of the wrapper was started. This event cannot be used by platform
 * plugins or extensions, as this event is fired before they get enabled.
 *
 * @since 4.0
 */
public final class ApplicationPostStartEvent extends Event {

  private final Class<?> applicationMainClass;
  private final Thread applicationThread;
  private final ClassLoader classLoader;

  /**
   * Constructs a new ApplicationPostStartEvent instance.
   *
   * @param applicationMainClass the main class instance which will be invoked to start the application.
   * @param applicationThread    the thread in which the application was started.
   * @param classLoader          the class loader which loaded the application main class.
   * @throws NullPointerException if the given app main, app thread or class loader is null.
   */
  public ApplicationPostStartEvent(
    @NonNull Class<?> applicationMainClass,
    @NonNull Thread applicationThread,
    @NonNull ClassLoader classLoader
  ) {
    this.applicationMainClass = applicationMainClass;
    this.applicationThread = applicationThread;
    this.classLoader = classLoader;
  }

  /**
   * Get the main class which was invoked when starting the application.
   *
   * @return the invoked main class of the application.
   */
  public @NonNull Class<?> applicationMainClass() {
    return this.applicationMainClass;
  }

  /**
   * Get the thread in which the application is running.
   *
   * @return the thread of the application.
   */
  public @NonNull Thread applicationThread() {
    return this.applicationThread;
  }

  /**
   * Get the class loader which was used to load the main class of the application (and all other classes of the
   * application in the runtime unless the application decides to switch to another loader).
   *
   * @return the class loader of the application main class.
   */
  public @NonNull ClassLoader classLoader() {
    return this.classLoader;
  }
}
