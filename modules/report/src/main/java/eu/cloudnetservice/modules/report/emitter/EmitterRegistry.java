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

package eu.cloudnetservice.modules.report.emitter;

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import java.util.Collection;
import java.util.Collections;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The EmitterRegistry keeps track of all {@link ReportDataEmitter}s that are used to collect data for reports.
 *
 * @since 4.0
 */
public class EmitterRegistry {

  private final Multimap<Class<?>, ReportDataEmitter> emitters = LinkedListMultimap.create();

  /**
   * Get all emitters which are registered.
   *
   * @return an unmodifiable collection of all emitters.
   */
  @UnmodifiableView
  public @NonNull Collection<ReportDataEmitter> emitters() {
    return Collections.unmodifiableCollection(this.emitters.values());
  }

  /**
   * Get all emitters which are registered for the given type, ignoring whether they are for a specific type or not.
   *
   * @param type the type of the emitters to get, {@code Object.class} for unspecific emitters.
   * @return an unmodifiable collection of all emitters for the given type.
   */
  @UnmodifiableView
  public @NonNull Collection<ReportDataEmitter> emitters(@NonNull Class<?> type) {
    return Collections.unmodifiableCollection(this.emitters.get(type));
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
    return this.emitters.get(type).stream()
      .filter(emitter -> emitter instanceof SpecificReportDataEmitter<?>)
      .map(emitter -> (SpecificReportDataEmitter<T>) emitter)
      .toList();
  }

  /**
   * Registers the given emitter. This method does not accept registration of emitters for specific report data, use the
   * method {@link #registerSpecificEmitter(Class, SpecificReportDataEmitter)} for that purpose instead.
   *
   * @param emitter the emitters to register.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException     if the given emitter is null.
   * @throws IllegalArgumentException if the given emitter can report specific data.
   */
  public @NonNull EmitterRegistry registerEmitter(@NonNull Class<? extends ReportDataEmitter> emitter) {
    var injectionLayer = InjectionLayer.findLayerOf(emitter);
    return this.registerEmitter(injectionLayer.instance(emitter));
  }

  /**
   * Registers the given emitter. This method does not accept registration of emitters for specific report data, use the
   * method {@link #registerSpecificEmitter(Class, SpecificReportDataEmitter)} for that purpose instead.
   *
   * @param emitter the emitters to register.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException     if the given emitter is null.
   * @throws IllegalArgumentException if the given emitter can report specific data.
   */
  public @NonNull EmitterRegistry registerEmitter(@NonNull ReportDataEmitter emitter) {
    Preconditions.checkArgument(!(emitter instanceof SpecificReportDataEmitter<?>));
    this.emitters.put(Object.class, emitter);
    return this;
  }

  /**
   * Registers the given specific data emitter for the given type.
   *
   * @param type    the raw type of data emitted by the given emitter.
   * @param emitter the emitter to register.
   * @param <T>     the type of data emitted by the given emitter.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given type or emitter is null.
   */
  public @NonNull <T> EmitterRegistry registerSpecificEmitter(
    @NonNull Class<T> type,
    @NonNull Class<? extends SpecificReportDataEmitter<T>> emitter
  ) {
    var injectionLayer = InjectionLayer.findLayerOf(emitter);
    return this.registerSpecificEmitter(type, injectionLayer.instance(emitter));
  }

  /**
   * Registers the given specific data emitter for the given type.
   *
   * @param type    the raw type of data emitted by the given emitter.
   * @param emitter the emitter to register.
   * @param <T>     the type of data emitted by the given emitter.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given type or emitter is null.
   */
  public @NonNull <T> EmitterRegistry registerSpecificEmitter(
    @NonNull Class<T> type,
    @NonNull SpecificReportDataEmitter<T> emitter
  ) {
    this.emitters.put(type, emitter);
    return this;
  }

  /**
   * Unregisters all emitters that are registered in this registry for the given type. {@code Object.class} represents
   * all emitters which are not made to report information about a specific type of data.
   *
   * @param type the type of the emitters to unregister.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given type is null.
   */
  public @NonNull EmitterRegistry unregisterByReportType(@NonNull Class<?> type) {
    this.emitters.removeAll(type);
    return this;
  }

  /**
   * Unregisters all emitters that were loaded by the given class loader instance. This does only apply to the emitter
   * instance, not to the class it's registered for.
   *
   * @param loader the class loader of the emitter instances to remove.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given class loader is null.
   */
  public @NonNull EmitterRegistry unregisterByEmitterClassLoader(@NonNull ClassLoader loader) {
    for (var entry : this.emitters.entries()) {
      if (entry.getValue().getClass().getClassLoader() == loader) {
        this.emitters.remove(entry.getKey(), entry.getValue());
      }
    }
    return this;
  }

  /**
   * Unregisters all known emitters from this registry.
   *
   * @return the same instance as used to call the method, for chaining.
   */
  public @NonNull EmitterRegistry unregisterAll() {
    this.emitters.clear();
    return this;
  }
}
