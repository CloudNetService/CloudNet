/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.platforminject.api;

import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface PlatformPluginManager<I, T> {

  @NonNull Collection<PlatformPluginInfo<I, T, ?>> loadedPlugins();

  @Nullable PlatformPluginInfo<I, T, ?> loadedPlugin(@NonNull T platformData);

  @Nullable PlatformPluginInfo<I, T, ?> loadedPluginById(@NonNull I id);

  void constructAndLoad(@NonNull Class<? extends PlatformEntrypoint> pluginClass, @NonNull T platformData);

  void disablePlugin(@NonNull T platformData);

  void disablePluginById(@NonNull I id);
}
