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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Logger that traces usages of unsafe methods when trace logging is enabled.
 *
 * @since 4.0
 */
@ApiStatus.Internal
@SuppressWarnings("deprecation")
public final class UnsafeUsageTraceLogger {

  /**
   * True if unsafe usages should be traced, false otherwise.
   */
  private static final boolean TRACE_UNSAFE_USAGE = Boolean.getBoolean("cloudnet.wrapper.trace-unsafe-usage");
  /**
   * Stack walker to get the caller of the unsafe replacement method.
   */
  private static final Supplier<StackWalker> CALLER_GET_STACK_WALKER =
    new LazyMemoizingSupplier<>(() -> StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE));

  private UnsafeUsageTraceLogger() {
    throw new UnsupportedOperationException();
  }

  /**
   * Traces the usage of an unsafe method when trace logging is enabled.
   *
   * @param calledMethodName the name of the unsafe method that is being called.
   * @param calledMethodDesc the descriptor of the unsafe method that is being called.
   * @throws NullPointerException if the given called method name or descriptor is null.
   */
  public static void traceUnsafeUsage(@NonNull String calledMethodName, @NonNull String calledMethodDesc) {
    if (TRACE_UNSAFE_USAGE && UnsafeLogUtil.debugEnabled()) {
      var stackWalker = CALLER_GET_STACK_WALKER.get();
      var callingFrame = stackWalker.walk(stream -> stream
        .skip(1) // skip this method
        .dropWhile(frame -> {
          var dc = frame.getDeclaringClass();
          return dc == UnsafeReplacementDelegate.class || dc.getName().equals("sun.misc.Unsafe");
        })
        .findFirst()
        .orElse(null));
      if (callingFrame != null) {
        UnsafeLogUtil.debug(
          "{}.{}{} (line {}) called unsafe method {}{}",
          callingFrame.getClassName(),
          callingFrame.getMethodName(),
          callingFrame.getDescriptor(),
          callingFrame.getLineNumber(),
          calledMethodName,
          calledMethodDesc);
      }
    }
  }
}
