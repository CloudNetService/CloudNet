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

package eu.cloudnetservice.driver.impl.network.rpc.handler.util;

import eu.cloudnetservice.driver.impl.network.rpc.listener.RPCPacketListener;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.exception.RPCExecutionException;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

/**
 * An internal utility class to communicate serialized exceptions thrown during RPC handling to the calling component.
 *
 * @since 4.0
 */
public final class RPCExceptionUtil {

  // stacktrace element array used for all exceptions without a stacktrace captured on the remote
  // exposed for some internal places that need an empty stack as well
  public static final StackTraceElement[] UNASSIGNED_STACK = new StackTraceElement[0];

  // magic constants that define the class and method cutoff point when serializing stack traces
  private static final String RPC_LISTENER_HANDLE_METHOD = "postRPCRequestToHandler";
  private static final String RPC_LISTENER_CLASS_BIN_NAME = RPCPacketListener.class.getName();

  private RPCExceptionUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Serializes the thrown handling exception into the given target data buffer.
   *
   * @param target    the buffer to serialize the exception into.
   * @param throwable the throwable to serialize.
   * @return the given buffer, for chaining.
   * @throws NullPointerException if the given buffer or throwable is null.
   */
  @Contract("_, _ -> param1")
  public static @NonNull DataBuf serializeHandlingException(
    @NonNull DataBuf.Mutable target,
    @NonNull Throwable throwable
  ) {
    // write type and message
    target
      .writeString(throwable.getClass().getSimpleName())
      .writeNullable(throwable.getMessage(), DataBuf.Mutable::writeString);

    var stacktrace = throwable.getStackTrace();
    if (stacktrace.length == 0) {
      // no stacktrace elements to serialize
      return target.writeInt(0);
    } else {
      // find the index from which the serialization should be done
      // this is the index of the frame in which the method invocation is requested by the RPC packet handler
      var cutoffIndex = -1;
      for (var index = stacktrace.length - 1; index >= 0; index--) {
        var stacktraceElement = stacktrace[index];
        if (stacktraceElement.getClassName().equals(RPC_LISTENER_CLASS_BIN_NAME)
          && stacktraceElement.getMethodName().equals(RPC_LISTENER_HANDLE_METHOD)) {
          cutoffIndex = index;
          break;
        }
      }
      if (cutoffIndex != -1) {
        // cutoff found point found, serialize all stack elements from there
        target.writeInt(cutoffIndex);
        for (var index = 0; index < cutoffIndex; index++) {
          var stacktraceElement = stacktrace[index];
          serializeStacktraceElement(target, stacktraceElement);
        }
      } else {
        // not sure where the cutoff point is, just serialize all
        // stacktrace  elements to not potentially lose any information
        target.writeInt(stacktrace.length);
        for (var stacktraceElement : stacktrace) {
          serializeStacktraceElement(target, stacktraceElement);
        }
      }
    }

    return target;
  }

  /**
   * Rethrows the exception that is serialized in the given buffer, wrapped in an {@link RPCExecutionException}. This
   * method never returns, it always throws an exception.
   *
   * @param source the buffer to read the exception information from.
   * @throws NullPointerException  if the given source is null.
   * @throws RPCExecutionException to rethrow the serialized exception, always.
   */
  @Contract("_ -> fail")
  public static void rethrowHandlingException(@NonNull DataBuf source) {
    // read type and message
    var exceptionClassName = source.readString();
    var exceptionMessage = source.readNullable(DataBuf::readString, "<no message provided>");

    // read stack trace
    var stacktraceElementCount = source.readInt();
    if (stacktraceElementCount == 0) {
      // no stacktrace present
      var executionException = new RPCExecutionException(exceptionClassName, exceptionMessage);
      executionException.setStackTrace(UNASSIGNED_STACK);
      throw executionException;
    } else {
      // read each stacktrace element & assign it before throwing the exception
      var stacktrace = new StackTraceElement[stacktraceElementCount];
      for (int index = 0; index < stacktraceElementCount; index++) {
        var classloaderName = source.readNullable(DataBuf::readString);
        var moduleName = source.readNullable(DataBuf::readString);
        var className = source.readString();
        var methodName = source.readString();
        var fileName = source.readNullable(DataBuf::readString);
        var lineNumber = source.readInt();

        var stacktraceElement = new StackTraceElement(
          classloaderName,
          moduleName,
          null,
          className,
          methodName,
          fileName,
          lineNumber);
        stacktrace[index] = stacktraceElement;
      }

      var executionException = new RPCExecutionException(exceptionClassName, exceptionMessage);
      executionException.setStackTrace(stacktrace);
      throw executionException;
    }
  }

  /**
   * Serializes a single stacktrace element into the given target data buffer.
   *
   * @param target  the buffer to serialize the stack element into.
   * @param element the stack element to serialize.
   * @throws NullPointerException if the given target buffer or stack element is null.
   */
  private static void serializeStacktraceElement(
    @NonNull DataBuf.Mutable target,
    @NonNull StackTraceElement element
  ) {
    target
      .writeNullable(element.getClassLoaderName(), DataBuf.Mutable::writeString)
      .writeNullable(element.getModuleName(), DataBuf.Mutable::writeString)
      .writeString(element.getClassName())
      .writeString(element.getMethodName())
      .writeNullable(element.getFileName(), DataBuf.Mutable::writeString)
      .writeInt(element.getLineNumber());
  }
}
