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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassAllocationUtilTest {

  @Test
  void usingLookupAllocatorIfNoArgsConstructorExists() {
    var allocator = ClassAllocationUtil.makeInstanceFactory(ClassWithNoArgsConstructor.class);
    var allocatedInstance = Assertions.assertDoesNotThrow(allocator::get);
    var instanceOfType = Assertions.assertInstanceOf(ClassWithNoArgsConstructor.class, allocatedInstance);
    Assertions.assertNotNull(instanceOfType.testValue);
    Assertions.assertEquals("hello world!", instanceOfType.testValue);
  }

  @Test
  void usingLookupAllocatorOnRecordWithNoArgsConstructor() {
    var allocator = ClassAllocationUtil.makeInstanceFactory(TestingRecordWithNoArgsConstructor.class);
    var allocatedInstance = Assertions.assertDoesNotThrow(allocator::get);
    var instanceOfType = Assertions.assertInstanceOf(TestingRecordWithNoArgsConstructor.class, allocatedInstance);
    Assertions.assertNotNull(instanceOfType.testValue());
    Assertions.assertEquals("hello world!", instanceOfType.testValue());
  }

  @Test
  void usingUnsafeAllocatorIfOnlyConstructorWithArgsExists() {
    var allocator = ClassAllocationUtil.makeInstanceFactory(ClassWithoutNoArgsConstructor.class);
    var allocatedInstance = Assertions.assertDoesNotThrow(allocator::get);
    var instanceOfType = Assertions.assertInstanceOf(ClassWithoutNoArgsConstructor.class, allocatedInstance);
    Assertions.assertNull(instanceOfType.testValue);
  }

  @Test
  void usingUnsafeAllocatorOnRecordWithoutNoArgsConstructor() {
    var allocator = ClassAllocationUtil.makeInstanceFactory(TestingRecordWithoutNoArgsConstructor.class);
    var allocatedInstance = Assertions.assertDoesNotThrow(allocator::get);
    var instanceOfType = Assertions.assertInstanceOf(TestingRecordWithoutNoArgsConstructor.class, allocatedInstance);
    Assertions.assertNull(instanceOfType.testValue());
  }

  public static final class ClassWithNoArgsConstructor {

    private final String testValue;

    public ClassWithNoArgsConstructor() {
      this.testValue = "hello world!";
    }
  }

  public static final class ClassWithoutNoArgsConstructor {

    private final String testValue;

    public ClassWithoutNoArgsConstructor(String testValue) {
      this.testValue = "hello world!";
    }
  }

  public record TestingRecordWithNoArgsConstructor(String testValue) {

    public TestingRecordWithNoArgsConstructor() {
      this("hello world!");
    }
  }

  public record TestingRecordWithoutNoArgsConstructor(String testValue) {

  }
}
