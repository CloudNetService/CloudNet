/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.rpc.defaults.handler.util;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.exception.RPCExecutionException;
import lombok.NonNull;

/**
 * A utility class used to rethrow exceptions which happened during rpc on the executor side and got sent back to the
 * original caller.
 *
 * @since 4.0
 */
public final class ExceptionalResultUtil {

  private ExceptionalResultUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Serializes the given throwable into the given data buffer by writing the
   * <ol>
   *   <li>name of the exception which was thrown.
   *   <li>optionally the message of the throwable if given.
   *   <li>first stack trace element.
   * </ol>
   *
   * @param target    the buffer to serialize the exception to.
   * @param throwable the throwable to serialize into the buffer.
   * @return the same buffer used to call the method, for chaining.
   */
  public static @NonNull DataBuf serializeThrowable(@NonNull DataBuf.Mutable target, @NonNull Throwable throwable) {
    // write the class name and the message of the exception
    target
      .writeString(throwable.getClass().getSimpleName())
      .writeNullable(throwable.getMessage(), DataBuf.Mutable::writeString);
    // write the first stack trace element
    return serializeFirstElement(target, throwable.getStackTrace());
  }

  /**
   * Rethrows a serialized exception by wrapping the message and stack trace information into on message and using it as
   * a message in a {@code RPCExecutionException}.
   *
   * @param source the source buffer to read the information from.
   * @throws RPCExecutionException always, that is the point of this method.
   */
  public static void rethrowException(@NonNull DataBuf source) {
    // read the information about the exception name
    var exceptionClassName = source.readString();
    // read the information about the exception message (which may be null)
    var exceptionMessage = source.readNullable(DataBuf::readString, "<no message provided>");
    // read & format the stack trace element
    var formattedElement = source.readNullable(dataBuf -> {
      // Argument order (in the buffer & constructor):
      //   - class name
      //   - method name
      //   - file name (maybe null)
      //   - line number (-2 indicates a native method)
      var element = new StackTraceElement(
        source.readString(),
        source.readString(),
        source.readNullable(DataBuf::readString),
        source.readInt());
      return element.toString();
    }, "<no stack trace available>");
    // rethrow the exception wrapped
    throw new RPCExecutionException(exceptionClassName, exceptionMessage, formattedElement);
  }

  /**
   * Writes the first stack trace element into the given buffer using the following format:
   * <ol>
   *   <li>A boolean, indicating if a stack trace element is following, if false no other field will follow.
   *   <li>A string, the class name where the exception happened.
   *   <li>A string, the method name where the exception happened.
   *   <li>An optional string, the file name where the exception happened.
   *   <li>An integer, the line number where the exception happened.
   * </ol>
   *
   * @param target   the buffer to write the element to.
   * @param elements the stack trace elements.
   * @return the same buffer used to call the methods, for chaining.
   */
  private static @NonNull DataBuf serializeFirstElement(
    @NonNull DataBuf.Mutable target,
    StackTraceElement @NonNull [] elements
  ) {
    if (elements.length == 0) {
      // no first element, skip
      return target.writeBoolean(false);
    } else {
      // read the first element from the stack
      var element = elements[0];
      // serialize useful information for a possible re-throw on the other end
      // the order makes it easier to create a StackTraceElement from the information
      return target
        .writeBoolean(true)
        .writeString(element.getClassName())
        .writeString(element.getMethodName())
        .writeNullable(element.getFileName(), DataBuf.Mutable::writeString)
        .writeInt(element.getLineNumber());
    }
  }
}
