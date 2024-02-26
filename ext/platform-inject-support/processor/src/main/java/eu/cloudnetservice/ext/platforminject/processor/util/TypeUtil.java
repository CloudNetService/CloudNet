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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import lombok.NonNull;

public final class TypeUtil {

  public static final TypeName UNBOUNDED_WILDCARD = WildcardTypeName.subtypeOf(TypeName.OBJECT);

  private TypeUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull ClassName lookupClassName(@NonNull Supplier<Class<?>> classExtractor) {
    ClassName listenerClassName;
    try {
      // try to get the class name by invoking the value method directly
      var listenerClass = classExtractor.get();
      listenerClassName = ClassName.get(listenerClass);
    } catch (MirroredTypeException exception) {
      // try to get the class name from the type mirror
      var candidateName = TypeName.get(exception.getTypeMirror());
      if (!(candidateName instanceof ClassName)) {
        throw new IllegalStateException("Invalid type name; non-class: " + candidateName);
      }

      listenerClassName = (ClassName) candidateName;
    }

    // return the class
    return listenerClassName;
  }

  public static @NonNull Set<ClassName> lookupClassNames(@NonNull Supplier<Class<?>[]> classesExtractor) {
    Set<ClassName> providedClassNames;
    try {
      // try to get the provided classes from the annotation directly
      var providedClasses = classesExtractor.get();
      providedClassNames = Arrays.stream(providedClasses).map(ClassName::get).collect(Collectors.toSet());
    } catch (MirroredTypesException exception) {
      // try to get the class names from the type mirrors
      providedClassNames = exception.getTypeMirrors().stream()
        .map(TypeName::get)
        .map(typeName -> {
          // ensure that each type name is a class name
          if (typeName instanceof ClassName className) {
            return className;
          }
          // invalid type
          throw new IllegalStateException("Invalid type name; non-class: " + typeName);
        })
        .collect(Collectors.toSet());
    }

    // return the provided classes
    return providedClassNames;
  }

  public static @NonNull TypeName convertParamsToUnboundedWildcard(
    @NonNull ParameterizedTypeName parameterizedTypeName
  ) {
    // build the type array with all types set to an unbounded wildcard
    var typeParams = new TypeName[parameterizedTypeName.typeArguments.size()];
    Arrays.fill(typeParams, UNBOUNDED_WILDCARD);

    // build a new type name from it
    return ParameterizedTypeName.get(parameterizedTypeName.rawType, typeParams);
  }

  public static @NonNull TypeName erasure(@NonNull TypeName typeName) {
    if (typeName instanceof ClassName className) {
      // already a class, nothing to do
      return className;
    } else if (typeName instanceof ArrayTypeName arrayTypeName) {
      // get the raw component type of the array
      var rawComponentType = erasure(arrayTypeName.componentType);
      return ArrayTypeName.of(rawComponentType);
    } else if (typeName instanceof ParameterizedTypeName parameterizedTypeName) {
      // use the raw type of the parameterized type
      return parameterizedTypeName.rawType;
    } else if (typeName instanceof TypeVariableName typeVariableName) {
      // erase the first bound, if any
      var bounds = typeVariableName.bounds;
      return bounds.isEmpty() ? TypeName.OBJECT : erasure(bounds.get(0));
    } else if (typeName instanceof WildcardTypeName wildcardTypeName) {
      // get the type variable from the lower bounds, if not present use the first upper bound
      var lowerBounds = wildcardTypeName.lowerBounds;
      return erasure(lowerBounds.isEmpty() ? wildcardTypeName.upperBounds.get(0) : lowerBounds.get(0));
    } else {
      // unable to handle the type name
      throw new IllegalArgumentException("Unable to handle type name " + typeName);
    }
  }
}
