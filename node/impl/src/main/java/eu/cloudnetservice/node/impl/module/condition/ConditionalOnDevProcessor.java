/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.module.condition;

import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.node.module.condition.ConditionContext;
import eu.cloudnetservice.node.module.condition.ConditionProcessor;
import eu.cloudnetservice.node.module.condition.ConditionalOnDev;
import jakarta.inject.Singleton;
import java.lang.classfile.Annotation;
import lombok.NonNull;

/**
 * A condition processor for the {@code @ConditionalOnDev} annotation.
 *
 * @since 4.0
 */
@Singleton
@AutoService(services = ConditionProcessor.class, name = "eu.cloudnetservice.node.module.condition.ConditionalOnDev")
public final class ConditionalOnDevProcessor implements ConditionProcessor {

  private static final boolean DEV_MODE = Boolean.getBoolean("cloudnet.dev");

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Class<? extends java.lang.annotation.Annotation> annotation() {
    return ConditionalOnDev.class;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean matches(@NonNull ConditionContext context, @NonNull Annotation matchedAnnotation) {
    return DEV_MODE;
  }
}
