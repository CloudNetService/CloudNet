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

package de.dytanic.cloudnet.common.encrypt;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

/**
 * Shortcut class to guava hashing methods.
 */
public final class EncryptTo {

  private EncryptTo() {
    throw new UnsupportedOperationException();
  }

  /**
   * Hashes the given string to sha256.
   *
   * @param text the text to hash.
   * @return the same input text hashed with the sha256 algorithm.
   */
  public static byte[] encryptToSHA256(String text) {
    return Hashing.sha256().hashString(text, StandardCharsets.UTF_8).asBytes();
  }

  /**
   * Encrypts the following byte array to an SHA-256 hash
   *
   * @param bytes the following bytes which should encrypt
   * @return the output SHA-256 hash
   */
  public static byte[] encryptToSHA256(byte[] bytes) {
    return Hashing.sha256().hashBytes(bytes).asBytes();
  }

  /**
   * Hashes the given string to sha1.
   *
   * @param text the text to hash.
   * @return the same input text hashed with the sha1 algorithm.
   * @deprecated This hashing method is everything but secure. It is not used internally anymore and will be removed.
   * Use {@link #encryptToSHA256(String)} instead.
   */
  @Deprecated
  public static byte[] encryptToSHA1(String text) {
    return Hashing.sha1().hashString(text, StandardCharsets.UTF_8).asBytes();
  }

  /**
   * Hashes the given string to sha1.
   *
   * @param bytes the text as bytes to hash.
   * @return the same input text hashed with the sha1 algorithm.
   * @deprecated This hashing method is everything but secure. It is not used internally anymore and will be removed.
   * Use {@link #encryptToSHA256(byte[])} instead.
   */
  @Deprecated
  public static byte[] encryptToSHA1(byte[] bytes) {
    return Hashing.sha1().hashBytes(bytes).asBytes();
  }
}
