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

package eu.cloudnetservice.ext.platforminject.processor.platform.limbo;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.processor.classgen.MethodBasedMainClassGenerator;
import javax.lang.model.element.TypeElement;
import lombok.NonNull;

final class LimboMainClassGenerator extends MethodBasedMainClassGenerator {

  private static final TypeName LIMBO_PLUGIN_CLASS_NAME = ClassName.get("com.loohp.limbo.plugins", "LimboPlugin");

  public LimboMainClassGenerator() {
    super("limbo", "onEnable", "onDisable");
  }

  @Override
  protected void customizeType(@NonNull ParsedPluginData pluginData, @NonNull TypeSpec.Builder typeBuilder) {
    typeBuilder.superclass(LIMBO_PLUGIN_CLASS_NAME);
  }

  @Override
  protected @NonNull CodeBlock providePlatformAccess(@NonNull TypeElement main, @NonNull ParsedPluginData data) {
    return CodeBlock.of("this");
  }

  @Override
  protected @NonNull CodeBlock provideClassLoaderAccess(@NonNull TypeElement main, @NonNull ParsedPluginData data) {
    return CodeBlock.of("this.getClass().getClassLoader().getParent()");
  }
}
