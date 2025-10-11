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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UnsafeReplacementMappingTest {

  @Test
  void testAllDeprecatedFieldsHaveAReplacement() {
    var replacementMapping = Assertions.assertDoesNotThrow(UnsafeReplacementMapping::load);
    var unsafeClass = Assertions.assertDoesNotThrow(() -> Class.forName("sun.misc.Unsafe"));
    var unsafeFields = unsafeClass.getDeclaredFields(); // don't test inherited fields
    for (var unsafeField : unsafeFields) {
      var name = unsafeField.getName();
      var desc = ClassDesc.ofDescriptor(unsafeField.getType().descriptorString());
      if (unsafeField.isAnnotationPresent(Deprecated.class)) {
        var replacementFieldName = replacementMapping.replacementFieldName(name, desc);
        Assertions.assertNotNull(replacementFieldName, "Unsafe replacement field name for " + name + " missing");
      }
    }
  }

  @Test
  void testAllDeprecatedMethodsHaveAReplacement() {
    var replacementMapping = Assertions.assertDoesNotThrow(UnsafeReplacementMapping::load);
    var unsafeClass = Assertions.assertDoesNotThrow(() -> Class.forName("sun.misc.Unsafe"));
    var unsafeMethods = unsafeClass.getDeclaredMethods(); // don't test inherited methods
    for (var unsafeMethod : unsafeMethods) {
      var name = unsafeMethod.getName();
      var type = MethodType.methodType(unsafeMethod.getReturnType(), unsafeMethod.getParameterTypes());
      var desc = MethodTypeDesc.ofDescriptor(type.descriptorString());
      if (unsafeMethod.isAnnotationPresent(Deprecated.class)) {
        var replacementMethodName = replacementMapping.replacementMethodName(name, desc);
        Assertions.assertNotNull(replacementMethodName, "Unsafe replacement method name for " + name + " missing");
      }
    }
  }
}
