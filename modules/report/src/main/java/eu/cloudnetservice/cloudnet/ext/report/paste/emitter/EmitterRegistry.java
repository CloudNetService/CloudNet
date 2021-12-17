/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.ext.report.paste.emitter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The EmitterRegistry keeps track of all {@link ReportDataEmitter} that are used to collect data for reports.
 *
 * @author Aldin s. (0utplay@cloudnetservice.eu)
 */
public class EmitterRegistry {

  private final Multimap<Class<?>, ReportDataEmitter<?>> emitters = ArrayListMultimap.create();

  /**
   * @return an unmodifiable collection of all registered emitters.
   */
  @UnmodifiableView
  public @NotNull Collection<ReportDataEmitter<?>> getEmitters() {
    return Collections.unmodifiableCollection(this.emitters.values());
  }

  /**
   * @param clazz the class the emitters are registered for.
   * @param <T>   the type of the report, currently {@link de.dytanic.cloudnet.service.ICloudService} & {@link
   *              de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot} are supported
   * @return an unmodifiable collection of all emitters for the given class
   */
  @UnmodifiableView
  @SuppressWarnings("unchecked")
  public <T> @NotNull Collection<ReportDataEmitter<T>> getEmitters(@NotNull Class<T> clazz) {
    return this.emitters.get(clazz)
      .stream()
      .map(emitter -> (ReportDataEmitter<T>) emitter)
      .toList();
  }

  /**
   * Registers the given emitters for the given class in the registry. This is used to append data for a report.
   *
   * @param clazz   the class to register the emitter for
   * @param emitter the emitters for the given class appending data
   * @param <T>     the type of the report, currently {@link de.dytanic.cloudnet.service.ICloudService} & {@link
   *                de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot} are supported
   */
  public <T> void registerDataEmitter(@NotNull Class<T> clazz, @NotNull ReportDataEmitter<T>... emitter) {
    this.emitters.putAll(clazz, Arrays.asList(emitter));
  }

  /**
   * Unregisters all emitters that are registered in this registry using the given class.
   *
   * @param clazz the class that was used to register the emitters
   * @param <T>   the type of the registered emitters
   */
  public <T> void unregisterByClass(@NotNull Class<T> clazz) {
    this.emitters.removeAll(clazz);
  }

  /**
   * Unregisters all known emitters by clearing the backing map.
   */
  public void unregisterAll() {
    this.emitters.clear();
  }
}
