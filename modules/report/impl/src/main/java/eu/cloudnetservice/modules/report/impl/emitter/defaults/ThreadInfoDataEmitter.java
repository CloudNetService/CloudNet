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

package eu.cloudnetservice.modules.report.impl.emitter.defaults;

import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataEmitter;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataWriter;
import jakarta.inject.Singleton;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.EnumSet;
import java.util.Set;
import lombok.NonNull;

@Singleton
@AutoService(services = ReportDataEmitter.class, name = "ThreadInfo")
public final class ThreadInfoDataEmitter implements ReportDataEmitter {

  private static final int DUMP_STACK_DEPTH = 15;
  private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
  private static final Set<Thread.State> SLEEPING_THREAD_STATES = EnumSet.of(
    Thread.State.WAITING,
    Thread.State.BLOCKED,
    Thread.State.TIMED_WAITING);

  private static boolean checkSleepState(@NonNull ThreadInfo thread) {
    // test if the state indicates that the thread is sleeping
    if (SLEEPING_THREAD_STATES.contains(thread.getThreadState())) {
      return true;
    }

    // check via the last call on the stack trace
    var stacktrace = thread.getStackTrace();
    if (stacktrace.length == 0) {
      return false;
    }

    var element = stacktrace[0];
    var clazz = element.getClassName();
    var method = element.getMethodName();

    // this is not nice, but better than nothing :/
    return (clazz.equals("java.lang.Thread") && method.equals("yield")) ||
      (clazz.equals("jdk.internal.misc.Unsafe") && method.equals("park"));
  }

  private static @NonNull String formatStackElement(@NonNull StackTraceElement element) {
    var fileName = element.getFileName();
    var lineNumber = element.getLineNumber();

    return element.getClassName() + "." + element.getMethodName()
      + "(" + (element.isNativeMethod()
      ? "Native Method)"
      : (fileName != null && lineNumber >= 0
        ? fileName + ":" + lineNumber + ")"
        : (fileName != null ? "" + fileName + ")" : "Unknown Source)")));
  }

  @Override
  public @NonNull ReportDataWriter emitData(@NonNull ReportDataWriter writer) {
    // Threads (10):
    var threads = THREAD_MX_BEAN.dumpAllThreads(false, false, DUMP_STACK_DEPTH);
    writer = writer.beginSection(title -> title.appendString("Threads (").appendInt(threads.length).appendString("):"));

    for (var thread : threads) {
      writer
        // Common-Cleaner (sleeping); priority: 8; daemon
        .appendString(thread.getThreadName())
        .appendString(checkSleepState(thread) ? " (sleeping)" : "")
        .appendString("; priority: ")
        .appendInt(thread.getPriority())
        .appendString(thread.isDaemon() ? "; daemon" : "")
        .appendString(thread.isInNative() ? " (native)" : "")
        .appendNewline();

      // stack trace
      for (var stackElement : thread.getStackTrace()) {
        writer.indent().appendString(formatStackElement(stackElement)).appendNewline();
      }

      // end the tread info
      writer.appendNewline();
    }

    return writer.endSection();
  }
}
