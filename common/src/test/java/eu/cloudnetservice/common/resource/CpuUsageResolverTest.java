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

package eu.cloudnetservice.common.resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CpuUsageResolverTest {

  @Test
  void testCpuUsageResolve() {
    var processCpuUsage = Assertions.assertDoesNotThrow(CpuUsageResolver::processCpuLoad);
    Assertions.assertTrue(processCpuUsage >= -1 && processCpuUsage <= 100);

    var systemCpuUsage = Assertions.assertDoesNotThrow(CpuUsageResolver::systemCpuLoad);
    Assertions.assertTrue(systemCpuUsage >= -1 && systemCpuUsage <= 100);
  }
}
