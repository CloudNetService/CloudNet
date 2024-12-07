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

package eu.cloudnetservice.node.impl.command.defaults;

import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.internal.jakarta.JakartaBridge;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.incendo.cloud.injection.InjectionRequest;
import org.incendo.cloud.injection.InjectionService;
import org.jetbrains.annotations.Nullable;

@Singleton
final class AerogelInjectionService implements InjectionService<CommandSource> {

  @Override
  public @Nullable Object handle(@NonNull InjectionRequest<CommandSource> request) {
    try {
      // get the associated data from the input values
      var targetClass = request.injectedClass();
      var annotations = request.annotationAccessor().annotations();

      var element = annotations.stream()
        .filter(JakartaBridge::isQualifierAnnotation)
        .findFirst()
        .map(Element.forType(targetClass)::requireAnnotation)
        .orElseGet(() -> Element.forType(targetClass));
      var injectionLayer = InjectionLayer.findLayerOf(targetClass);

      // get the instance of the given class from the injection layer
      return injectionLayer.instance(element);
    } catch (Exception exception) {
      return null;
    }
  }
}
