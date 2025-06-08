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

// used by unsafe replacement test
final class ClassWithFields {

  private static final String STR = System.getProperty("___totally_existing___", "hello world"); // prevent inlining
  private static int intField = 1337;
  private static long longField = 696969;

  private final int instIntField;
  private char instCharField = 'A';
  private boolean instBoolField = false;
  private String instStringField = "final string";

  private ClassWithFields(int instIntField) {
    this.instIntField = instIntField;
  }

  public ClassWithFields() {
    this(123456789); // prevent inlining
  }

  Object[] getFieldValues() {
    return new Object[]{
      STR,
      intField,
      longField,
      this.instIntField,
      this.instStringField,
      this.instCharField,
      this.instBoolField,
    };
  }
}
