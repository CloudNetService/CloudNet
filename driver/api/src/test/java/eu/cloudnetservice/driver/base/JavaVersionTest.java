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

package eu.cloudnetservice.driver.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JavaVersionTest {

  @Test
  void testRuntimeVersion() {
    // we require java 23 to build (atm)
    var runtimeVersion = JavaVersion.runtimeVersion();
    Assertions.assertTrue(JavaVersion.JAVA_23.atOrAbove());
    Assertions.assertTrue(runtimeVersion.isNewerOrAt(JavaVersion.JAVA_23));
  }

  @Test
  void testNextAndUnsupportedAreHidden() {
    Assertions.assertTrue(JavaVersion.fromMajor(-1).isEmpty());
    Assertions.assertTrue(JavaVersion.fromMajor(Integer.MAX_VALUE).isEmpty());
    Assertions.assertTrue(JavaVersion.fromClassFileVersion(-1D).isEmpty());
    Assertions.assertTrue(JavaVersion.fromClassFileVersion(Double.MAX_VALUE).isEmpty());
  }

  @Test
  void testVersionRangeChecking() {
    Assertions.assertTrue(JavaVersion.JAVA_10.isInRange(JavaVersion.JAVA_9, JavaVersion.JAVA_14));
    Assertions.assertTrue(JavaVersion.JAVA_15.isInRange(JavaVersion.JAVA_8, JavaVersion.JAVA_16));
    Assertions.assertTrue(JavaVersion.JAVA_11.isInRange(JavaVersion.JAVA_11, JavaVersion.JAVA_17));

    Assertions.assertFalse(JavaVersion.JAVA_8.isInRange(JavaVersion.JAVA_9, JavaVersion.JAVA_14));
    Assertions.assertFalse(JavaVersion.JAVA_17.isInRange(JavaVersion.JAVA_18, JavaVersion.JAVA_21));
    Assertions.assertFalse(JavaVersion.JAVA_11.isInRange(JavaVersion.JAVA_17, JavaVersion.JAVA_21));
  }

  @Test
  void testVersionGuessing() {
    var newerVersion = JavaVersion.guessFromMajor(1290);
    Assertions.assertTrue(newerVersion.supported());
    Assertions.assertSame(JavaVersion.JAVA_NEXT, newerVersion);
    Assertions.assertTrue(newerVersion.isNewerOrAt(JavaVersion.JAVA_17));

    var unsupportedVersion = JavaVersion.guessFromMajor(4);
    Assertions.assertFalse(unsupportedVersion.supported());
    Assertions.assertSame(JavaVersion.JAVA_UNSUPPORTED, unsupportedVersion);
    Assertions.assertFalse(unsupportedVersion.isNewerOrAt(JavaVersion.JAVA_17));
    Assertions.assertFalse(unsupportedVersion.isOlderOrAt(JavaVersion.JAVA_17));

    var knownVersion = JavaVersion.guessFromMajor(16);
    Assertions.assertTrue(knownVersion.supported());
    Assertions.assertSame(JavaVersion.JAVA_16, knownVersion);
    Assertions.assertTrue(knownVersion.isNewerOrAt(JavaVersion.JAVA_16));
    Assertions.assertTrue(knownVersion.isNewerOrAt(JavaVersion.JAVA_11));
    Assertions.assertFalse(knownVersion.isNewerOrAt(JavaVersion.JAVA_17));
    Assertions.assertTrue(knownVersion.isOlderOrAt(JavaVersion.JAVA_16));
    Assertions.assertTrue(knownVersion.isOlderOrAt(JavaVersion.JAVA_17));
    Assertions.assertFalse(knownVersion.isOlderOrAt(JavaVersion.JAVA_14));
  }

  @Test
  void testResolveFromClassVersion() {
    var knownVersion = JavaVersion.fromClassFileVersion(57D);
    Assertions.assertTrue(knownVersion.isPresent());
    Assertions.assertSame(JavaVersion.JAVA_13, knownVersion.get());

    var unknownVersion = JavaVersion.fromClassFileVersion(23D);
    Assertions.assertTrue(unknownVersion.isEmpty());
  }

  @Test
  void testResolveFromMajor() {
    var knownVersion = JavaVersion.fromMajor(9);
    Assertions.assertTrue(knownVersion.isPresent());
    Assertions.assertSame(JavaVersion.JAVA_9, knownVersion.get());

    var unknownVersion = JavaVersion.fromMajor(3);
    Assertions.assertTrue(unknownVersion.isEmpty());
  }
}
