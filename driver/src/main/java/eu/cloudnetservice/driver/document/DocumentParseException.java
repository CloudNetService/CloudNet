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

package eu.cloudnetservice.driver.document;

import java.io.Serial;
import lombok.NonNull;

/**
 * An exception that gets thrown when an issue occurs while parsing a document from the given input data.
 *
 * @since 4.0
 */
public final class DocumentParseException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -3831380795139686659L;

  /**
   * Constructs a new document parse exception with the given cause.
   *
   * @param cause the cause of the exception.
   * @throws NullPointerException if the given cause is null.
   */
  public DocumentParseException(@NonNull Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new document parse exception with the given description message and cause.
   *
   * @param message the error message describing what happened.
   * @param cause   the cause of the exception.
   * @throws NullPointerException if the given message or cause is null.
   */
  public DocumentParseException(@NonNull String message, @NonNull Throwable cause) {
    super(message, cause);
  }
}
