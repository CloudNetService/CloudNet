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

package eu.cloudnetservice.driver.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.derklaro.reflexion.MethodAccessor;
import dev.derklaro.reflexion.Reflexion;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A utility class to easier access methods around executor services.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class ExecutorServiceUtil {

  // marker which indicates if virtual threads are available & preview features enabled
  private static final boolean VIRTUAL_THREADS_AVAILABLE;

  private static final MethodAccessor<Method> VIRTUAL_BUILDER_GETTER;
  private static final MethodAccessor<Method> VIRTUAL_BUILDER_NAME;
  private static final MethodAccessor<Method> VIRTUAL_BUILDER_TO_FACTORY;

  private static final MethodAccessor<Method> EXECUTORS_NEW_THREAD_PER_TASK;

  static {
    var threadReflexion = Reflexion.on(Thread.class);

    // try to get the Thread.ofVirtual method; invoke if present -> an exception is thrown = preview features are disabled
    VIRTUAL_BUILDER_GETTER = threadReflexion.findMethod("ofVirtual").orElse(null);
    if (VIRTUAL_BUILDER_GETTER == null || VIRTUAL_BUILDER_GETTER.invoke().wasExceptional()) {
      // virtual threads not available or preview not enabled
      VIRTUAL_THREADS_AVAILABLE = false;
      VIRTUAL_BUILDER_NAME = null;
      VIRTUAL_BUILDER_TO_FACTORY = null;
      EXECUTORS_NEW_THREAD_PER_TASK = null;
    } else {
      // virtual threads are available
      VIRTUAL_THREADS_AVAILABLE = true;

      // find the virtual builder class & the name(String, long) method
      var virtualBuilderReflexion = Reflexion.find("java.lang.Thread$Builder$OfVirtual").orElseThrow();
      VIRTUAL_BUILDER_NAME = virtualBuilderReflexion
        .findMethod("name", String.class, long.class)
        .orElseThrow();

      // find the factory() method
      VIRTUAL_BUILDER_TO_FACTORY = virtualBuilderReflexion.findMethod("factory").orElseThrow();

      // find the Executors.newThreadPerTaskExecutor(ThreadFactory) method
      var executorsReflexion = Reflexion.on(Executors.class);
      EXECUTORS_NEW_THREAD_PER_TASK = executorsReflexion
        .findMethod("newThreadPerTaskExecutor", ThreadFactory.class)
        .orElseThrow();
    }
  }

  private ExecutorServiceUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get if virtual threads are available and will be returned by {@link #newVirtualThreadExecutor(String, Function)}
   * (in other word if this jvm is running on java 19 or newer and has preview features enabled).
   *
   * @return true if virtual threads are available, false otherwise.
   */
  public static boolean virtualThreadsAvailable() {
    return VIRTUAL_THREADS_AVAILABLE;
  }

  /**
   * Creates a new virtual thread executor if the current jvm supports them (running on Java 19+ with preview features
   * enabled). If virtual threads are not supported a new thread factory based on the given name format will be built
   * and passed to the given fallback executor factory.
   *
   * @param threadNamePrefix        the prefix of the thread name, not including {@code %d} (for the thread id).
   * @param fallbackExecutorFactory the fallback executor service factory if virtual threads are not supported.
   * @return a new virtual thread based executor if possible, or a factory supplied by the fallback supplier.
   * @throws NullPointerException if the given name prefix or fallback executor factory is null.
   */
  public static @NonNull ExecutorService newVirtualThreadExecutor(
    @NonNull String threadNamePrefix,
    @NonNull Function<ThreadFactory, ExecutorService> fallbackExecutorFactory
  ) {
    if (VIRTUAL_THREADS_AVAILABLE) {
      // builds a new thread factory for virtual threads
      return VIRTUAL_BUILDER_GETTER.invoke()
        .flatMap(builder -> VIRTUAL_BUILDER_NAME.invoke(builder, threadNamePrefix, 1L))
        .flatMap(VIRTUAL_BUILDER_TO_FACTORY::invoke)
        .flatMap(EXECUTORS_NEW_THREAD_PER_TASK::<ExecutorService>invokeWithArgs)
        .getOrThrow();
    } else {
      var threadFactory = new ThreadFactoryBuilder()
        .setNameFormat(threadNamePrefix + "%d")
        .setThreadFactory(Executors.defaultThreadFactory())
        .build();
      return fallbackExecutorFactory.apply(threadFactory);
    }
  }
}
