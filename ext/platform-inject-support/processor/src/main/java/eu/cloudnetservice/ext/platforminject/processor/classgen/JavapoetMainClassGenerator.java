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

package eu.cloudnetservice.ext.platforminject.processor.classgen;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.api.generator.PlatformMainClassGenerator;
import java.io.IOException;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.NonNull;

public abstract class JavapoetMainClassGenerator implements PlatformMainClassGenerator {

  @Override
  public void generatePluginMainClass(
    @NonNull Filer target,
    @NonNull String packageName,
    @NonNull ParsedPluginData data,
    @NonNull TypeElement internalMain,
    @NonNull String platformMainClassName
  ) throws IOException {
    // build the type
    var typeBuilder = TypeSpec.classBuilder(platformMainClassName).addModifiers(Modifier.PUBLIC);
    this.generatePluginMainClass(data, internalMain, typeBuilder);

    // write the type to the filer
    var javaFile = JavaFile.builder(packageName, typeBuilder.build()).build();
    javaFile.writeTo(target);
  }

  protected abstract void generatePluginMainClass(
    @NonNull ParsedPluginData pluginData,
    @NonNull TypeElement internalMainClass,
    @NonNull TypeSpec.Builder typeBuilder);
}
