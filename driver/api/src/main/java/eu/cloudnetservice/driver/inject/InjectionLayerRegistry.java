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

package eu.cloudnetservice.driver.inject;

import dev.derklaro.aerogel.Injector;
import io.vavr.Tuple2;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A registry for injection layers which can be bound to specific hints.
 *
 * @since 4.0
 */
@ApiStatus.Internal
final class InjectionLayerRegistry {

  // use a linked queue here to prevent calls to hashCode on the hint - that could (depending on
  // the implementation) take a long time, which would need to be repeated for each call (when using a Map)
  private final Collection<Tuple2<Object, InjectionLayer<? extends Injector>>> knownLayers = new ConcurrentLinkedQueue<>();

  /**
   * Registers to this registry and maps the given hints to the layer. During the process, there will be no calls to
   * either equals or hashCode. For later lookups, the given hint must be exactly as one of the given hints (as defined
   * by the operator {@code ==}).
   * <p>
   * Note that layer registrations are ordered. If different layers are registered for the same hint instance, the first
   * one that was registered first will be returned.
   *
   * @param layer the layer to register to this registry.
   * @param hints the hints to associate the given layer with.
   * @throws NullPointerException if either the given layer or hints are null.
   */
  public void registerLayer(@NonNull InjectionLayer<? extends Injector> layer, @NonNull Object... hints) {
    for (Object hint : hints) {
      this.knownLayers.add(new Tuple2<>(hint, layer));
    }
  }

  /**
   * Unregisters all hints that were associated with the given layer. The layer to unregister is defined by the same
   * instance given to the method (as defined by the operator {@code ==}).
   *
   * @param layer the layer to unregister the hints of.
   * @throws NullPointerException if the gien layer is null.
   */
  public void unregisterLayer(@NonNull InjectionLayer<?> layer) {
    for (var iterator = this.knownLayers.iterator(); iterator.hasNext(); ) {
      var registeredLayer = iterator.next()._2();
      if (registeredLayer == layer) {
        iterator.remove();
      }
    }
  }

  /**
   * Finds the first layer which is exactly associated with the given hint (as defined by the operator {@code ==}).
   *
   * @param hint the hint to find the layer of.
   * @return the injection layer associated with the hint, or null if no layer is associated.
   * @throws NullPointerException if the given hint is null.
   */
  public @Nullable InjectionLayer<? extends Injector> findByHint(@Nullable Object hint) {
    for (var knownLayer : this.knownLayers) {
      // only return based on an exact match
      if (knownLayer._1() == hint) {
        return knownLayer._2();
      }
    }
    return null;
  }
}
