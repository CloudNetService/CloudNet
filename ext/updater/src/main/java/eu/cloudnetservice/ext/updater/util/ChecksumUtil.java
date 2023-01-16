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

package eu.cloudnetservice.ext.updater.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.NonNull;

public final class ChecksumUtil {

  private static final ThreadLocal<byte[]> READ_BUFFERS = ThreadLocal.withInitial(() -> new byte[8096]);

  private ChecksumUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull String fileShaSum(@NonNull Path path) {
    var readBuffer = READ_BUFFERS.get();
    try (var stream = new DigestInputStream(Files.newInputStream(path), newSha3256Digest())) {
      // clear the stream by just reading without doing anything
      //noinspection StatementWithEmptyBody
      while (stream.read(readBuffer) != -1) {
      }
      // calculate the result as hex
      return bytesToHex(stream.getMessageDigest().digest());
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to get sha3_256 digest of file " + path, exception);
    }
  }

  private static @NonNull MessageDigest newSha3256Digest() {
    try {
      return MessageDigest.getInstance("SHA3-256");
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to retrieve message digest for algorithm sha3-256", exception);
    }
  }

  private static @NonNull String bytesToHex(byte[] input) {
    var buffer = new StringBuilder();
    for (var b : input) {
      // This is indeed very simple to understand if you understand how bytes work. A byte is a number between -128 and
      // 127. This means that we're working with 8 bits. Writing a number is pretty easy, take for example the number 2:
      // 0000 0010. Since it is two's complement a negative 2 looks like this: 1111 1110. That's why converting to hex
      // is extremely simple. We can just convert each (of the two) for 4 bit segments directly to hex. (If you don't
      // know what two's complement is (which you need to know to understand this whole thing, read this nice article
      // about it: https://cs.cornell.edu/~tomf/notes/cps104/twoscomp.html)
      buffer.append(Character.forDigit((b >> 4) & 0xF, 16));
      buffer.append(Character.forDigit(b & 0xF, 16));
    }
    // convert to a string
    return buffer.toString();
  }
}
