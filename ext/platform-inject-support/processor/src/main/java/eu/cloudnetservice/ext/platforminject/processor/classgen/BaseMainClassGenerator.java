/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import eu.cloudnetservice.ext.platforminject.loader.PlatformInjectSupportLoader;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseMainClassGenerator extends JavapoetMainClassGenerator {

  protected final String platformName;

  protected BaseMainClassGenerator(@NonNull String platformName) {
    this.platformName = platformName;
  }

  protected @NonNull CodeBlock visitPluginLoad(
    @NonNull TypeElement main,
    @NonNull CodeBlock platformDataGetter,
    @NonNull CodeBlock platformClassLoaderResolver
  ) {
    return CodeBlock.of(
      "$T.loadPlugin($S, $T.class, $L, $L);",
      PlatformInjectSupportLoader.class,
      this.platformName,
      main,
      platformDataGetter,
      platformClassLoaderResolver);
  }

  protected @NonNull CodeBlock visitPluginDisableByData(@NonNull CodeBlock platformDataGetter) {
    return CodeBlock.of(
      "$T.disablePlugin($S, $L);",
      PlatformInjectSupportLoader.class,
      this.platformName,
      platformDataGetter);
  }

  protected @NonNull MethodSpec.Builder appendConstructListenerConstruction(
    @NonNull MethodSpec.Builder target,
    @NonNull CodeBlock dataAccessor,
    @Nullable String listenerClass
  ) {
    // check if a listener class is given
    if (listenerClass != null) {
      // try to resolve the class, in case that succeeds append the call to construct the class
      var className = ClassName.bestGuess(listenerClass);
      target.addCode(CodeBlock.of("new $T($L);", className, dataAccessor));
    }
    return target;
  }

  protected @NonNull MethodSpec.Builder beginMethod(@NonNull String name) {
    return MethodSpec.methodBuilder(name).addModifiers(Modifier.PUBLIC);
  }
}
