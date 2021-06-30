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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class includes simple hash operations, which should use, to sign data or some other operations.
 */
public final class EncryptTo {

  private EncryptTo() {
    throw new UnsupportedOperationException();
  }

  /**
   * Invokes the encryptToSHA256(byte[] bytes) method
   */
  public static byte[] encryptToSHA256(String text) {
    return encryptToSHA256(text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Encrypts the following byte array to an SHA-256 hash
   *
   * @param bytes the following bytes which should encrypt
   * @return the output SHA-256 hash
   */
  public static byte[] encryptToSHA256(byte[] bytes) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(bytes);
      return messageDigest.digest();
    } catch (NoSuchAlgorithmException exception) {
      exception.printStackTrace();
    }

    return null;
  }

  /**
   * Invokes the encryptToSHA1(byte[] bytes) method
   */
  public static byte[] encryptToSHA1(String text) {
    return encryptToSHA1(text.getBytes(StandardCharsets.UTF_8));
  }


  /**
   * Encrypts the following byte array to an SHA-1 hash
   *
   * @param bytes the following bytes which should encrypt
   * @return the output SHA-1 hash
   */
  public static byte[] encryptToSHA1(byte[] bytes) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
      messageDigest.update(bytes);
      return messageDigest.digest();
    } catch (NoSuchAlgorithmException exception) {
      exception.printStackTrace();
    }

    return null;
  }
}
