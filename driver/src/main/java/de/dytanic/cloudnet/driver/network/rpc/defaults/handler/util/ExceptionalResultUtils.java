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

package de.dytanic.cloudnet.driver.network.rpc.defaults.handler.util;

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.exception.RPCExecutionException;
import lombok.NonNull;

public final class ExceptionalResultUtils {

  private ExceptionalResultUtils() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull DataBuf serializeThrowable(@NonNull DataBuf.Mutable target, @NonNull Throwable throwable) {
    // write the class name and the message of the exception
    target
      .writeString(throwable.getClass().getSimpleName())
      .writeNullable(throwable.getMessage(), DataBuf.Mutable::writeString);
    // write the first stack trace element
    return serializeFirstElement(target, throwable.getStackTrace());
  }

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
