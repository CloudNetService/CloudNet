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

package eu.cloudnetservice.utils.base.resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResourceFormatterTest {

  @Test
  void testTwoDigitFormat() {
    Assertions.assertEquals("100.0", ResourceFormatter.formatTwoDigitPrecision(100));
    Assertions.assertEquals("98.49", ResourceFormatter.formatTwoDigitPrecision(98.493));
    Assertions.assertEquals("56.89", ResourceFormatter.formatTwoDigitPrecision(56.887));
  }

  @Test
  void testPercentageFormatting() {
    Assertions.assertEquals(89D, ResourceFormatter.convertToPercentage(0.89D));
    Assertions.assertEquals(100D, ResourceFormatter.convertToPercentage(100D));
    Assertions.assertEquals(45.25D, ResourceFormatter.convertToPercentage(0.4525D));
  }

  @Test
  void testConvertBytesToMegabytes() {
    Assertions.assertEquals(9, ResourceFormatter.convertBytesToMb(10_000_000));
    Assertions.assertEquals(10, ResourceFormatter.convertBytesToMb(10_485_760));
    Assertions.assertEquals(95, ResourceFormatter.convertBytesToMb(100_000_000));
  }
}
