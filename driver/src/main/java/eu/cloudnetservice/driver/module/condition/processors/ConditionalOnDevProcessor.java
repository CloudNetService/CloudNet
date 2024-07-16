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

package eu.cloudnetservice.driver.module.condition.processors;

import eu.cloudnetservice.driver.module.condition.ConditionContext;
import eu.cloudnetservice.driver.module.condition.ConditionProcessor;
import jakarta.inject.Singleton;
import java.lang.classfile.Annotation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A condition processor for the {@code @ConditionalOnDev} annotation.
 *
 * @since 4.0
 */
@Singleton
@ApiStatus.Internal
public final class ConditionalOnDevProcessor implements ConditionProcessor {

  private static final boolean DEV_MODE = Boolean.getBoolean("cloudnet.dev");

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean matches(@NotNull ConditionContext context, @NotNull Annotation matchedAnnotation) {
    return DEV_MODE;
  }
}
