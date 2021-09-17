/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.network.rpc.exception;

import de.dytanic.cloudnet.driver.network.rpc.util.ReflectionUtils;
import java.lang.reflect.Field;
import org.jetbrains.annotations.NotNull;

public class MissingFieldGetterException extends IllegalStateException {

  public MissingFieldGetterException(@NotNull Class<?> sourceClass, @NotNull Field field) {
    super(String.format(
      "Missing getter method for field %s from super serialisation class %s",
      ReflectionUtils.fieldToReadableString(field),
      sourceClass.getCanonicalName()));
  }
}
