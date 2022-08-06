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

package eu.cloudnetservice.driver.network.exception;

import io.netty5.handler.codec.DecoderException;
import lombok.NonNull;

/**
 * A subtype of a decoder exception indicating that something went wrong during decoding. This exception will not
 * generate a stack trace as it is not needed for further understanding what happened, the message should be unique.
 * <p>
 * Instances of this exception should get cached as there is no stack trace (and therefore only cluttering of new
 * instances which all have one thing in common: no difference in information).
 *
 * @since 4.0
 */
public class SilentDecoderException extends DecoderException {

  /**
   * Constructs a new silent decoder exception instance.
   *
   * @param message the message why the exception happened, should be unique.
   * @throws NullPointerException if the given message is null.
   */
  public SilentDecoderException(@NonNull String message) {
    super(message);
  }

  /**
   * Overridden method to prevent filling the stack trace. This method call does nothing.
   *
   * @return the same instance as used to call the method, without any change made to it.
   */
  @Override
  public Throwable fillInStackTrace() {
    return this;
  }

  @Override
  public synchronized Throwable initCause(Throwable cause) {
    return this;
  }
}
