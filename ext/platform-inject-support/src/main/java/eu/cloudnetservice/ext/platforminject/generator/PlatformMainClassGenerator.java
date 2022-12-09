/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.platforminject.generator;

import com.squareup.javapoet.TypeSpec;
import eu.cloudnetservice.ext.platforminject.data.ParsedPluginData;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

public abstract class PlatformMainClassGenerator {

  public @NonNull TypeSpec generatePluginMainClass(
    @NonNull ParsedPluginData data,
    @NonNull TypeElement internalMain,
    @NonNull String platformMainClassName
  ) {
    var typeBuilder = TypeSpec.classBuilder(platformMainClassName).addModifiers(Modifier.PUBLIC);
    this.generatePluginMainClass(data, internalMain, typeBuilder);
    return typeBuilder.build();
  }

  @ApiStatus.OverrideOnly
  protected abstract void generatePluginMainClass(
    @NonNull ParsedPluginData pluginData,
    @NonNull TypeElement internalMainClass,
    @NonNull TypeSpec.Builder typeBuilder);
}
