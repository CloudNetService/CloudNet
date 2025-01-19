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

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResourceResolverTest {

  @Test
  void testNormalClassResolve() {
    var codeSource = ResourceResolver.resolveCodeSourceOfClass(ResourceResolverTest.class);
    Assertions.assertNotNull(codeSource);
    Assertions.assertEquals("file", codeSource.getScheme());

    var convertedPath = Assertions.assertDoesNotThrow(() -> Path.of(codeSource));
    Assertions.assertTrue(Files.exists(convertedPath));
  }

  @Test
  void testJavaBaseClassResolve() {
    var codeSource = ResourceResolver.resolveCodeSourceOfClass(Class.class);
    Assertions.assertNotNull(codeSource);
    Assertions.assertEquals("jrt", codeSource.getScheme());

    var convertedPath = Assertions.assertDoesNotThrow(() -> Path.of(codeSource));
    Assertions.assertTrue(Files.exists(convertedPath));
  }
}
