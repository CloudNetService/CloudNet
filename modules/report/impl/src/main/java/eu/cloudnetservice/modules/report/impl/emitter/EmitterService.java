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

package eu.cloudnetservice.modules.report.impl.emitter;

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.registry.ServiceRegistryRegistration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The EmitterService keeps track of all {@link ReportDataEmitter}s that are used to collect data for reports.
 *
 * @since 4.0
 */
@Singleton
public class EmitterService {

  private final ServiceRegistry serviceRegistry;

  @Inject
  public EmitterService(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Get all emitters which are registered.
   *
   * @return an unmodifiable collection of all emitters.
   */
  @UnmodifiableView
  public @NonNull Collection<ReportDataEmitter> emitters() {
    return this.serviceRegistry.registrations(ReportDataEmitter.class)
      .stream()
      .map(ServiceRegistryRegistration::serviceInstance)
      .toList();
  }

  /**
   * Get all emitters which are registered for the given type, ignoring whether they are for a specific type or not.
   *
   * @param type the type of the emitters to get, {@code Object.class} for unspecific emitters.
   * @return an unmodifiable collection of all emitters for the given type.
   */
  @UnmodifiableView
  public @NonNull Collection<ReportDataEmitter> emitters(@NonNull Class<?> type) {
    return this.emitters().stream().filter(emitter -> emitter.emittingType() == type).toList();
  }

  /**
   * Get all emitters which are registered for the given type.
   *
   * @param type the raw type of data emitted by the emitter to retrieve.
   * @param <T>  the type of data emitted by the emitter.
   * @return an unmodifiable collection of all specific emitters for the given type.
   */
  @UnmodifiableView
  @SuppressWarnings("unchecked")
  public @NonNull <T> Collection<SpecificReportDataEmitter<T>> specificEmitters(@NonNull Class<T> type) {
    return this.serviceRegistry.registrations(ReportDataEmitter.class).stream()
      .map(ServiceRegistryRegistration::serviceInstance)
      .filter(emitter -> emitter instanceof SpecificReportDataEmitter<?>)
      .filter(emitter -> emitter.emittingType() == type)
      .map(emitter -> (SpecificReportDataEmitter<T>) emitter)
      .toList();
  }
}
