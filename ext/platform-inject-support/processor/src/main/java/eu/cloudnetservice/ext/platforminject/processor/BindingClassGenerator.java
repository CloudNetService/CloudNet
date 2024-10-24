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

package eu.cloudnetservice.ext.platforminject.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import eu.cloudnetservice.ext.platforminject.api.inject.BindingsInstaller;
import eu.cloudnetservice.ext.platforminject.processor.util.GeantyrefUtil;
import eu.cloudnetservice.ext.platforminject.processor.util.TypeUtil;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import lombok.NonNull;

final class BindingClassGenerator {

  private static final ClassName INJECTION_LAYER = ClassName.get("eu.cloudnetservice.driver.inject", "InjectionLayer");
  private static final TypeName GENERIC_INJECTION_LAYER = ParameterizedTypeName.get(
    INJECTION_LAYER,
    TypeUtil.UNBOUNDED_WILDCARD);

  public static @NonNull TypeSpec buildBindingClass(
    @NonNull String className,
    @NonNull Collection<ParsedBindingData> bindingData
  ) {
    // override the applyBindings method
    var applyBindings = MethodSpec.methodBuilder("applyBindings")
      .addAnnotation(Override.class)
      .addModifiers(Modifier.PUBLIC)
      .addParameter(GENERIC_INJECTION_LAYER, "l");

    applyBindings.addCode(CodeBlock.of("var builder = l.injector().createBindingBuilder();"));

    // apply each binding
    for (var binding : bindingData) {
      // build the layer install code block for the raw types
      var rawTypesGetter = binding.providingElements().stream()
        .map(type -> CodeBlock.of("$T.class", type))
        .collect(Collectors.toList());

      // build the layer install block for the generic types, if any
      var genericElements = binding.providedGenericElements();
      if (!genericElements.isEmpty()) {
        var genericTypesGetter = genericElements.stream()
          .flatMap(typeName -> {
            // build the full generic type constructor for the type, return early if that is not possible
            var genericTypeConstructor = GeantyrefUtil.buildConstructorFor(typeName);
            if (genericTypeConstructor == null) {
              return null;
            }

            // if the current type is parameterized, also bind the generic version of it (if requested)
            if (binding.bindWildcardTypes() && typeName instanceof ParameterizedTypeName parameterizedTypeName) {
              var unboundedType = TypeUtil.convertParamsToUnboundedWildcard(parameterizedTypeName);
              var unboundedTypeConstructor = GeantyrefUtil.buildConstructorFor(unboundedType);

              // add the unbounded element, if present
              if (unboundedTypeConstructor != null) {
                return Stream.of(genericTypeConstructor, unboundedTypeConstructor);
              }
            }

            // only add the generic constructor
            return Stream.of(genericTypeConstructor);
          }).toList();


        rawTypesGetter.addAll(genericTypesGetter);
      }

      var layerInstallRawBlock = buildLayerInstallBlock(rawTypesGetter, binding.boundElement());
      applyBindings.addCode(layerInstallRawBlock);
    }

    // build the class
    return TypeSpec.classBuilder(className)
      .addSuperinterface(BindingsInstaller.class)
      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
      .addMethod(applyBindings.build())
      .build();
  }

  private static @NonNull CodeBlock buildLayerInstallBlock(
    @NonNull List<CodeBlock> bindingBlocks,
    @NonNull ClassName boundElement
  ) {
    // build the block which actually adds the binding to the layer
    var constructorBuild = CodeBlock.builder().add("builder.bind($L)", bindingBlocks.getFirst());
    if (bindingBlocks.size() > 1) {
      for (int i = 1; i < bindingBlocks.size(); i++) {
        constructorBuild.add(".andBind($L)", bindingBlocks.get(i));
      }
    }

    constructorBuild.add(".toConstructingClass($T.class)", boundElement);
    return CodeBlock.of("l.install($L);", constructorBuild.build());
  }

  public record ParsedBindingData(
    boolean bindWildcardTypes,
    @NonNull String packageName,
    @NonNull String platform,
    @NonNull ClassName boundElement,
    @NonNull Set<ClassName> providingElements,
    @NonNull Set<TypeName> providedGenericElements
  ) {

  }
}
