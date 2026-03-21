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

package eu.cloudnetservice.node.impl.module.metadata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class UnknownModuleVersionTest {

  @ParameterizedTest
  @ValueSource(strings = {"", "test-test", "hello_world", "test.testing", "WTF.???"})
  void testCanParseVersionStringWithoutNumbers(String versionString) {
    var version = Assertions.assertDoesNotThrow(() -> UnknownModuleVersion.parse(versionString));
    Assertions.assertEquals(0, version.major());
    Assertions.assertEquals(0, version.minor());
    Assertions.assertEquals(0, version.patch());
    Assertions.assertEquals("", version.build());
    Assertions.assertEquals("", version.preRelease());
    Assertions.assertEquals(versionString, version.displayString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"4.7.2-hello_world??", "build+4.7.2???", "what.4.is.7.this.2.???", "4.7.2"})
  void testCanExtractVersionFromSomewhereInString(String versionString) {
    var version = Assertions.assertDoesNotThrow(() -> UnknownModuleVersion.parse(versionString));
    Assertions.assertEquals(4, version.major());
    Assertions.assertEquals(7, version.minor());
    Assertions.assertEquals(2, version.patch());
    Assertions.assertEquals("", version.build());
    Assertions.assertEquals("", version.preRelease());
    Assertions.assertEquals(versionString, version.displayString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"4.-hello_world??", "build+4???", "what.4.is.this.???", "4"})
  void testCanExtractPartialVersionNumbers1(String versionString) {
    var version = Assertions.assertDoesNotThrow(() -> UnknownModuleVersion.parse(versionString));
    Assertions.assertEquals(4, version.major());
    Assertions.assertEquals(0, version.minor());
    Assertions.assertEquals(0, version.patch());
    Assertions.assertEquals("", version.build());
    Assertions.assertEquals("", version.preRelease());
    Assertions.assertEquals(versionString, version.displayString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"4.2-hello_world??", "build+4.2???", "what.4.2.???", "4.2"})
  void testCanExtractPartialVersionNumbers2(String versionString) {
    var version = Assertions.assertDoesNotThrow(() -> UnknownModuleVersion.parse(versionString));
    Assertions.assertEquals(4, version.major());
    Assertions.assertEquals(2, version.minor());
    Assertions.assertEquals(0, version.patch());
    Assertions.assertEquals("", version.build());
    Assertions.assertEquals("", version.preRelease());
    Assertions.assertEquals(versionString, version.displayString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"4.2.5.26.265", "4.what.2.am.5.I.26.doing.2.here.65.???", "4.2.5::???.262---65"})
  void testExcessNumbersAreMergedIntoSinglePatchVersion(String versionString) {
    var version = Assertions.assertDoesNotThrow(() -> UnknownModuleVersion.parse(versionString));
    Assertions.assertEquals(4, version.major());
    Assertions.assertEquals(2, version.minor());
    Assertions.assertEquals(526265, version.patch());
    Assertions.assertEquals("", version.build());
    Assertions.assertEquals("", version.preRelease());
    Assertions.assertEquals(versionString, version.displayString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"2-8-12", "2,8,12", "2_8_12", "2.8.12"})
  void testCanParseVersionsWithWeirdSeparators(String versionString) {
    var version = Assertions.assertDoesNotThrow(() -> UnknownModuleVersion.parse(versionString));
    Assertions.assertEquals(2, version.major());
    Assertions.assertEquals(8, version.minor());
    Assertions.assertEquals(12, version.patch());
    Assertions.assertEquals("", version.build());
    Assertions.assertEquals("", version.preRelease());
    Assertions.assertEquals(versionString, version.displayString());
  }
}
