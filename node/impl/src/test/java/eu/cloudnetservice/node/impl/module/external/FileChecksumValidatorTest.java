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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileChecksumValidatorTest {

  @TempDir
  static Path tempDir;

  @Test
  void testAllChecksumAlgorithmsActuallyExist() {
    for (var algorithm : FileChecksumValidator.ALGORITHM_MAPPING.values()) {
      Assertions.assertDoesNotThrow(() -> MessageDigest.getInstance(algorithm), algorithm);
    }
  }

  @Test
  void testSuccessfulChecksumValidation() throws IOException {
    var validator = new FileChecksumValidator(HexFormat.of());
    var input = "The irony of the situation wasn't lost on anyone in the room.";
    // @formatter:off
    var expectedResults = Map.of(
      "md5", "031c5069ed1639f7dba2416309594851",
      "sha1", "3cec38ba7aa2fa149b4fab99098552d916c4159b",
      "sha256", "01e18e153ef05aafeeaf86f1d744d3e2a51fab02fcc333fe1bd94fec22283297",
      "sha512", "01bb32ff21950012133ecd1c393cfa700165dfdcb7fb42872b270c84eb1307ce117d0a859ed5afa6ec934154ae4d3a8d0b005b4751a2902e842ad708c2cc964b",
      "sha3256", "988dc780619316d36e4518b60fb282ab9c09dc2ea067d7392bdfa55b1676f15f",
      "sha3512", "7225ddbe02269eb4fb6ecd76d20a5e1809a82062c8bfc2e26132b32b38b7c40b074c168e1dca93c1af67e0af2f485e8a10e7b38936c08e5dd6f6e172fb0e7246",
      "shake128", "0d1f434aba2b54b8dbb9e55d95010ee18585e0048c2851b4bc8e554115ec36ea",
      "shake256", "15b407a2203b0e031fb868a24f32ef1c8ea573257348fd63d32166e6242d7af0cf989c78c673504e7680bc9c44ec4f4a4c17f54fb50ad1c9a9780e1fbc48809b"
    );
    // @formatter:on
    for (var entry : expectedResults.entrySet()) {
      var testFilePath = tempDir.resolve(UUID.randomUUID().toString());
      Files.writeString(testFilePath, input, StandardCharsets.UTF_8);
      Assertions.assertTrue(validator.validateFileChecksum(entry, testFilePath));
    }
  }

  @Test
  void testFailingChecksumValidation() throws IOException {
    var validator = new FileChecksumValidator(HexFormat.of());
    var input = "Two things are infinite: the universe and human stupidity; and I'm not sure about the universe.";
    // @formatter:off
    var expectedResults = Map.of(
      "md5", "031c5069ed1639f7dba2416309594851",
      "sha1", "3cec38ba7aa2fa149b4fab99098552d916c4159b",
      "sha256", "01e18e153ef05aafeeaf86f1d744d3e2a51fab02fcc333fe1bd94fec22283297",
      "sha512", "01bb32ff21950012133ecd1c393cfa700165dfdcb7fb42872b270c84eb1307ce117d0a859ed5afa6ec934154ae4d3a8d0b005b4751a2902e842ad708c2cc964b",
      "sha3256", "988dc780619316d36e4518b60fb282ab9c09dc2ea067d7392bdfa55b1676f15f",
      "sha3512", "7225ddbe02269eb4fb6ecd76d20a5e1809a82062c8bfc2e26132b32b38b7c40b074c168e1dca93c1af67e0af2f485e8a10e7b38936c08e5dd6f6e172fb0e7246",
      "shake128", "0d1f434aba2b54b8dbb9e55d95010ee18585e0048c2851b4bc8e554115ec36ea",
      "shake256", "15b407a2203b0e031fb868a24f32ef1c8ea573257348fd63d32166e6242d7af0cf989c78c673504e7680bc9c44ec4f4a4c17f54fb50ad1c9a9780e1fbc48809b"
    );
    // @formatter:on
    for (var entry : expectedResults.entrySet()) {
      var testFilePath = tempDir.resolve(UUID.randomUUID().toString());
      Files.writeString(testFilePath, input, StandardCharsets.UTF_8);
      Assertions.assertFalse(validator.validateFileChecksum(entry, testFilePath));
    }
  }
}
