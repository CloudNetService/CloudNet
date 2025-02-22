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
import java.util.Collection;
import lombok.NonNull;

/**
 * An event called when the underlying application of the wrapper is about to be started. This event cannot be used by
 * platform plugins or extensions, as this event is fired before they get enabled.
 *
 * @since 4.0
 */
public final class ApplicationPreStartEvent extends Event {

  private final Class<?> applicationMainClass;
  private final Collection<String> arguments;
  private final ClassLoader classLoader;

  /**
   * Constructs a new ApplicationPreStartEvent instance.
   *
   * @param applicationMainClass the main class instance which will be invoked to start the application.
   * @param arguments            the process arguments which will be passed to the application.
   * @param classLoader          the class loader which loaded the application main class.
   * @throws NullPointerException if the given application main, arguments or class loader is null.
   */
  public ApplicationPreStartEvent(
    @NonNull Class<?> applicationMainClass,
    @NonNull Collection<String> arguments,
    @NonNull ClassLoader classLoader
  ) {
    this.applicationMainClass = applicationMainClass;
    this.arguments = arguments;
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
   * Get the arguments which will be passed to the application when starting. The returned collection is modifiable and
   * can be used to change the arguments before starting.
   *
   * @return the arguments which will be passed to the application as process arguments.
   */
  public @NonNull Collection<String> arguments() {
    return this.arguments;
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
