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

package eu.cloudnetservice.driver.impl.network.object;

import dev.derklaro.aerogel.binding.BindingBuilder;
import eu.cloudnetservice.driver.inject.BootLayerConfigurator;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import lombok.NonNull;

/**
 * Registers the default object mapper instance to the boot injection layer.
 *
 * @since 4.0
 */
public final class ObjectMapperInjectRegistration implements BootLayerConfigurator {

  /**
   * {@inheritDoc}
   */
  @Override
  public void configureBootLayer(@NonNull InjectionLayer<?> bootLayer) {
    var bindingConstructor = BindingBuilder.create()
      .bind(ObjectMapper.class)
      .bindFully(DefaultObjectMapper.class)
      .toInstance(DefaultObjectMapper.DEFAULT_MAPPER);
    bootLayer.install(bindingConstructor);
  }
}
