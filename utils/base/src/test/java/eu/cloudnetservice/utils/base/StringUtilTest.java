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

package eu.cloudnetservice.utils.base;

import java.security.SecureRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class StringUtilTest {

  @RepeatedTest(100)
  void testGenerateRandomString() {
    var stringLength = Math.abs(new SecureRandom().nextInt(100) + 1);
    var randomString = StringUtil.generateRandomString(stringLength);

    Assertions.assertNotNull(randomString);
    Assertions.assertEquals(stringLength, randomString.length());
  }

  @RepeatedTest(100)
  void testEndsWithIgnoreCase() {
    var randomString = StringUtil.generateRandomString(20);
    var randomStringSuffix = randomString.substring(15);

    Assertions.assertTrue(StringUtil.endsWithIgnoreCase(randomString, randomStringSuffix));
  }

  @RepeatedTest(100)
  void testStartsWithIgnoreCase() {
    var randomString = StringUtil.generateRandomString(20);
    var randomStringPrefix = randomString.substring(0, 15);

    Assertions.assertTrue(StringUtil.startsWithIgnoreCase(randomString, randomStringPrefix));
  }

  @RepeatedTest(100)
  void testCharRepeat() {
    var length = Math.abs(new SecureRandom().nextInt(100) + 1);
    var repeated = StringUtil.repeat(' ', length);

    Assertions.assertEquals(repeated.length(), length);
  }
}
