/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread that executes the main method of the wrapped application and stays alive until the application exits, either
 * normally or by throwing an exception.
 *
 * @since 4.0
 */
final class ApplicationThread extends Thread {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationThread.class);

  private static final int LOGGED_ERROR_EXIST_STATUS = -1;
  private static final int UNLOGGED_ERROR_EXIST_STATUS = -2;

  private final Method mainMethod;
  private final String[] mainArgs;

  /**
   * Constructs and setups the application thread for execution.
   *
   * @param mainMethod the main method to use for running the wrapped application.
   * @param mainArgs   the arguments to pass to the wrapped main method.
   * @throws NullPointerException if the given main method or main args collection is null.
   */
  public ApplicationThread(@NonNull Method mainMethod, @NonNull Collection<String> mainArgs) {
    this.mainMethod = mainMethod;
    this.mainArgs = mainArgs.toArray(String[]::new);

    // explicitly disable daemon mode for the thread to ensure it keeps the wrapper
    // alive until the wrapped application terminates in any way
    super.setDaemon(false);
    super.setName("Application-Thread");
    super.setPriority(Thread.NORM_PRIORITY + 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    try {
      this.mainMethod.invoke(null, new Object[]{this.mainArgs});
      LOGGER.info("Invocation of application main method {} completed successfully", this.mainMethod);
      return;
    } catch (InvocationTargetException exception) {
      // application threw error during execution
      var cause = exception.getCause();
      var exceptionToHandle = Objects.requireNonNullElse(cause, exception);
      LOGGER.error("Caught application exception while running {}", this.mainMethod, exceptionToHandle);
    } catch (IllegalArgumentException | NullPointerException exception) {
      // illegal invocation of the given main method due to argument type mismatch
      LOGGER.error("[BUG] Unable to invoke main method {} of application: {}", this.mainMethod, exception.getMessage());
    } catch (IllegalAccessException exception) {
      // illegal access to the main method, possibly private or in a module
      LOGGER.error(
        "The main method {} of the application cannot be called because the access modifiers of method are too strict: {}",
        this.mainMethod, exception.getMessage());
    } catch (Exception exception) {
      LOGGER.error("Caught unexpected exception while running {}", this.mainMethod, exception);
    } catch (Throwable _) {
      // assume the worst case situation if no other catch clause handled the exception yet
      // immediately exit the vm without even trying to log something (logging might fail as well)
      Runtime.getRuntime().halt(UNLOGGED_ERROR_EXIST_STATUS);
    }

    // fall-through case for handled exceptions
    System.exit(LOGGED_ERROR_EXIST_STATUS);
  }
}
