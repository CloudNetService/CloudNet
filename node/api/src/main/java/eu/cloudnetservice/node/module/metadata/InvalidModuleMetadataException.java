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

package eu.cloudnetservice.node.module.metadata;

import java.io.Serial;
import lombok.NonNull;

/**
 * Exception thrown to indicate that a file does contain a parsable metadata file, but the content of the file are
 * invalid. For example, this exception is thrown if a module does not contain a module id.
 *
 * @since 4.0
 */
public class InvalidModuleMetadataException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 8035431250116319922L;

  /**
   * Constructs a new invalid module metadata exception.
   *
   * @param message the detail message why the metadata is invalid.
   * @throws NullPointerException if the given message is null.
   */
  public InvalidModuleMetadataException(@NonNull String message) {
    super(message, null); // disallow initCause()
  }
}
