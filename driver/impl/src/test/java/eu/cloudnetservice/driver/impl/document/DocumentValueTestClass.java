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

package eu.cloudnetservice.driver.impl.document;

import eu.cloudnetservice.driver.document.annotations.DocumentFieldRename;
import eu.cloudnetservice.driver.document.annotations.DocumentValueIgnore;

public record DocumentValueTestClass(SomeInnerClass innerClass, SomeExcludedInnerClass excludedInnerClass) {

  public record SomeInnerClass(
    @DocumentFieldRename("worldHello") String helloWorld,
    @DocumentValueIgnore String fullyIgnoredField,
    @DocumentValueIgnore(DocumentValueIgnore.Direction.SERIALIZE) String serializedIgnoredField,
    @DocumentValueIgnore(DocumentValueIgnore.Direction.DESERIALIZE) String deserializedIgnoredField
  ) {

  }

  @DocumentValueIgnore(DocumentValueIgnore.Direction.SERIALIZE)
  public static final class SomeExcludedInnerClass {

  }
}
