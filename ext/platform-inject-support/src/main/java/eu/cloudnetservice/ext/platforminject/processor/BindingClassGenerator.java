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

package eu.cloudnetservice.ext.platforminject.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import eu.cloudnetservice.ext.platforminject.inject.BindingsInstaller;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;
import javax.lang.model.element.Modifier;
import lombok.NonNull;

final class BindingClassGenerator {

  private static final ClassName INJECTION_LAYER = ClassName.get("eu.cloudnetservice.driver.inject", "InjectionLayer");
  private static final TypeName GENERIC_INJECTION_LAYER = ParameterizedTypeName.get(
    INJECTION_LAYER,
    WildcardTypeName.subtypeOf(TypeName.OBJECT));

  private static final ClassName BINDING_BUILDER = ClassName.get("dev.derklaro.aerogel.binding", "BindingBuilder");

  public static @NonNull TypeSpec buildBindingClass(
    @NonNull String className,
    @NonNull Collection<ParsedBindingData> bindingData
  ) {
    // override the applyBindings method
    var applyBindings = MethodSpec.methodBuilder("applyBindings")
      .addAnnotation(Override.class)
      .addModifiers(Modifier.PUBLIC)
      .addParameter(GENERIC_INJECTION_LAYER, "l");

    // apply each binding
    for (var binding : bindingData) {
      // build the array block of the provided types
      var typesBlock = binding.providingElements().stream()
        .map(type -> CodeBlock.of("$T.class", type))
        .collect(CodeBlock.joining(", ", "{", "}"));

      // build the block which actually adds the binding to the layer
      var constructorBuild = CodeBlock.of("$T.create().bindAllFully(new $T[]$L).toConstructing($T.class)",
        BINDING_BUILDER,
        Type.class,
        typesBlock,
        binding.boundElement());
      var block = CodeBlock.of("l.install($L);", constructorBuild);

      // add the line to the method
      applyBindings.addCode(block);
    }

    // build the class
    return TypeSpec.classBuilder(className)
      .addSuperinterface(BindingsInstaller.class)
      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
      .addMethod(applyBindings.build())
      .build();
  }

  public record ParsedBindingData(
    @NonNull String packageName,
    @NonNull String platform,
    @NonNull ClassName boundElement,
    @NonNull Set<ClassName> providingElements
  ) {

  }
}
