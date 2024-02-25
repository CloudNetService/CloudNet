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

package eu.cloudnetservice.ext.platforminject.processor.util;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class GeantyrefUtil {

  private static final ClassName TYPE_FACTORY = ClassName.get("io.leangen.geantyref", "TypeFactory");

  private GeantyrefUtil() {
    throw new UnsupportedOperationException();
  }

  public static @Nullable CodeBlock buildConstructorFor(@NonNull TypeName typeName) {
    if (typeName instanceof ClassName) {
      // simple class name, no magic
      return CodeBlock.of("$T.class", typeName);
    } else if (typeName instanceof ArrayTypeName arrayTypeName) {
      var componentTypeFactory = buildConstructorFor(arrayTypeName.componentType);
      return CodeBlock.of("$T.arrayOf($L)", TYPE_FACTORY, componentTypeFactory);
    } else if (typeName instanceof ParameterizedTypeName parameterizedTypeName) {
      // get the factory for the raw type
      var rawTypeGetter = buildConstructorFor(parameterizedTypeName.rawType);

      // get the code block to get the value of each type argument
      List<CodeBlock> typeArgumentGetter = new LinkedList<>();
      for (var typeArgument : parameterizedTypeName.typeArguments) {
        var argumentTypeFactory = buildConstructorFor(typeArgument);
        typeArgumentGetter.add(argumentTypeFactory);
      }

      // build the factory
      return CodeBlock.of(
        "$T.parameterizedClass($L, new $T[]{$L})",
        TYPE_FACTORY,
        rawTypeGetter,
        Type.class,
        CodeBlock.join(typeArgumentGetter, ", "));
    } else if (typeName instanceof WildcardTypeName wildcardTypeName) {
      var lowerBounds = wildcardTypeName.lowerBounds;
      if (lowerBounds.size() == 1) {
        // ? super ...
        var boundFactory = buildConstructorFor(lowerBounds.get(0));
        return CodeBlock.of("$T.wildcardSuper($L)", TYPE_FACTORY, boundFactory);
      }

      // check if the type is a generic wildcard
      var upperBounds = wildcardTypeName.upperBounds;
      if (upperBounds.get(0).equals(TypeName.OBJECT)) {
        // generic wildcard
        return CodeBlock.of("$T.unboundWildcard()", TYPE_FACTORY);
      } else {
        // ? extends ...
        var boundFactory = buildConstructorFor(upperBounds.get(0));
        return CodeBlock.of("$T.wildcardExtends($L)", TYPE_FACTORY, boundFactory);
      }
    } else {
      // unable to handle the given type name
      return null;
    }
  }
}
