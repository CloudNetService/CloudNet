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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import eu.cloudnetservice.wrapper.impl.transform.ClassTransformer;
import java.io.IOException;
import java.lang.classfile.ClassFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UnsafeTransformerTest {

  @Test
  void testUnsafeTransformer() throws IOException {
    var classLoader = ClassLoader.getSystemClassLoader();
    try (var unsafeInputStream = classLoader.getResourceAsStream("sun/misc/Unsafe.class")) {
      Assertions.assertNotNull(unsafeInputStream);
      var unsafeClassBytes = unsafeInputStream.readAllBytes();

      var transformer = Assertions.assertDoesNotThrow(UnsafeTransformer::new);
      var transformWillingness = transformer.classTransformWillingness("sun/misc/Unsafe");
      Assertions.assertEquals(ClassTransformer.TransformWillingness.ACCEPT_ONCE, transformWillingness);

      var classFile = ClassFile.of();
      var classModel = classFile.parse(unsafeClassBytes);
      Assertions.assertDoesNotThrow(
        () -> classFile.transformClass(classModel, transformer.provideClassTransform(classModel)));
    }
  }
}
