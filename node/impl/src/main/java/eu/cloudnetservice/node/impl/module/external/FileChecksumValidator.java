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

package eu.cloudnetservice.node.impl.module.external;

import com.google.common.base.Verify;
import com.google.common.base.VerifyException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Validation utility for file checksums.
 *
 * @since 4.0
 */
final class FileChecksumValidator {

  /**
   * Mapping of the supported algorithms for checksum calculation. The key is the name that must be supplied by the
   * source, the value is the name to retrieve a message digest instance based on.
   */
  @VisibleForTesting
  static final Map<String, String> ALGORITHM_MAPPING = Map.of(
    "md5", "MD5",
    "sha1", "SHA-1",
    "sha256", "SHA-256",
    "sha512", "SHA-512",
    "sha3256", "SHA3-256",
    "sha3512", "SHA3-512",
    "shake128", "SHAKE128",
    "shake256", "SHAKE256"
  );

  private final HexFormat hexFormat;

  public FileChecksumValidator(@NonNull HexFormat hexFormat) {
    this.hexFormat = hexFormat;
  }

  /**
   * Validates the checksum of the file at the given path against the given checksum data. The checksum entry must carry
   * the expected checksum algorithm as the key and the expected checksum as the value.
   *
   * @param checksumData the checksum data to validate the file content against.
   * @param filePath     the path to the file to validate the checksum of.
   * @return true if the checksum of the given file matches the checksum in the given data, false otherwise.
   * @throws NullPointerException if the given checksum data or file path is null.
   * @throws VerifyException      if the given checksum algorithm is unsupported.
   * @throws UncheckedIOException if an i/o exception occurs while reading from the file at the given path.
   */
  public boolean validateFileChecksum(@NonNull Map.Entry<String, String> checksumData, @NonNull Path filePath) {
    try {
      var digestAlgorithm = ALGORITHM_MAPPING.get(checksumData.getKey());
      Verify.verifyNotNull(
        digestAlgorithm,
        "Checksum validation request using unsupported algorithm '%s'",
        checksumData.getKey());

      var digest = MessageDigest.getInstance(digestAlgorithm);
      var fileChecksum = this.calculateFileChecksum(filePath, digest);
      return fileChecksum.equals(checksumData.getValue());
    } catch (NoSuchAlgorithmException exception) {
      throw new AssertionError("invalid message digest algorithm: " + checksumData.getKey());
    } catch (IOException exception) {
      var msg = String.format("failed to read file @%s for checksum validation", filePath);
      throw new UncheckedIOException(msg, exception);
    }
  }

  /**
   * Calculates the checksum of the file at the given path using the given message digest.
   *
   * @param filePath the path to the file to calculate the checksum of.
   * @param digest   the message digest to use for checksum calculation.
   * @return the checksum of the given file, hex-encoded.
   * @throws NullPointerException if the given file path or message digest is null.
   * @throws IOException          if an i/o error occurs while reading from the file at the given path.
   */
  @NonNull
  private String calculateFileChecksum(@NonNull Path filePath, @NonNull MessageDigest digest) throws IOException {
    try (var digestStream = new DigestInputStream(Files.newInputStream(filePath), digest)) {
      digestStream.transferTo(OutputStream.nullOutputStream()); // consumes the full stream
      return this.hexFormat.formatHex(digest.digest());
    }
  }
}
