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

package eu.cloudnetservice.driver.impl.registry;

import dev.derklaro.aerogel.binding.BindingBuilder;
import eu.cloudnetservice.driver.inject.BootLayerConfigurator;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import io.leangen.geantyref.GenericTypeReflector;
import lombok.NonNull;

/**
 * Registers a binding for the {@link Service} annotation into the boot injection layer.
 */
// used by SPI
public final class ServiceAnnotationInjectConfigurator implements BootLayerConfigurator {

  /**
   * {@inheritDoc}
   */
  @Override
  public void configureBootLayer(@NonNull InjectionLayer<?> bootLayer) {
    var bindingConstructor = BindingBuilder.create()
      .bindMatching(element -> {
        // ensure that the element has at least one special requirement (like the Service annotation)
        if (element.hasSpecialRequirements()) {
          return element.requiredAnnotations()
            .stream()
            .anyMatch(predicate -> predicate.annotationType().equals(Service.class));
        } else {
          return false;
        }
      }).toLazyProvider((element, _) -> {
        var serviceType = GenericTypeReflector.erase(element.componentType());
        var serviceName = element.requiredAnnotations().stream()
          .filter(predicate -> predicate.annotationType().equals(Service.class))
          .findFirst()
          .map(annotationPredicate -> {
            var annotationValues = annotationPredicate.annotationValues();
            return (String) annotationValues.get("name");
          })
          .orElseThrow(); // does not throw as the annotation must be present here
        if (serviceName.isBlank()) {
          // default service requested
          return () -> {
            var registry = ServiceRegistry.registry();
            return registry.defaultInstance(serviceType);
          };
        } else {
          // specific service requested
          return () -> {
            var registry = ServiceRegistry.registry();
            return registry.instance(serviceType, serviceName);
          };
        }
      });
    bootLayer.install(bindingConstructor);
  }
}
