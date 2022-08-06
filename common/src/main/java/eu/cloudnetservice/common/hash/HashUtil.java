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

package eu.cloudnetservice.common.hash;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Shortcut class to guava hashing methods.
 */
@ApiStatus.Internal
public final class HashUtil {

  private HashUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Hashes the given string to sha256.
   *
   * @param text the text to hash.
   * @return the same input text hashed with the sha256 algorithm.
   */
  public static byte @NonNull [] toSha256(@NonNull String text) {
    return Hashing.sha256().hashString(text, StandardCharsets.UTF_8).asBytes();
  }

  /**
   * Encrypts the following byte array to an SHA-256 hash
   *
   * @param bytes the following bytes which should encrypt
   * @return the output SHA-256 hash
   */
  public static byte @NonNull [] toSha256(byte @NonNull [] bytes) {
    return Hashing.sha256().hashBytes(bytes).asBytes();
  }
}
