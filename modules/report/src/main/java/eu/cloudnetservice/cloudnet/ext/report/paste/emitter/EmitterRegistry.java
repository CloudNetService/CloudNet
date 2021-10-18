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
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public class EmitterRegistry {

  private final Multimap<Class<?>, ReportDataEmitter<?>> emitters = ArrayListMultimap.create();

  @UnmodifiableView
  public @NotNull Collection<ReportDataEmitter<?>> getEmitters() {
    return Collections.unmodifiableCollection(this.emitters.values());
  }

  @UnmodifiableView
  @SuppressWarnings("unchecked")
  public <T> @NotNull Collection<ReportDataEmitter<T>> getEmitters(@NotNull Class<T> clazz) {
    return this.emitters.get(clazz).stream().map(emitter -> (ReportDataEmitter<T>) emitter)
      .collect(Collectors.toList());
  }

  public <T> void registerDataEmitter(@NotNull Class<T> clazz, @NotNull ReportDataEmitter<T>... emitter) {
    this.emitters.putAll(clazz, Arrays.asList(emitter));
  }
}
