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

package eu.cloudnetservice.modules.cloudperms.sponge.service.system;

import eu.cloudnetservice.modules.cloudperms.sponge.service.memory.InMemorySubjectCollection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

public final class SystemSubjectCollection extends InMemorySubjectCollection {

  public SystemSubjectCollection(String identifier, PermissionService service) {
    super(identifier, service);
  }

  @Override
  public CompletableFuture<? extends Subject> loadSubject(String identifier) {
    return CompletableFuture.completedFuture(new SystemSubject(identifier, this));
  }

  @Override
  public Optional<? extends Subject> subject(String identifier) {
    return Optional.of(new SystemSubject(identifier, this));
  }

  @Override
  public CompletableFuture<Boolean> hasSubject(String identifier) {
    return CompletableFuture.completedFuture(Boolean.TRUE);
  }

  @Override
  public CompletableFuture<? extends Map<String, ? extends Subject>> loadSubjects(Iterable<String> identifiers) {
    return CompletableFuture.completedFuture(StreamSupport.stream(identifiers.spliterator(), false)
      .collect(Collectors.toMap(
        Function.identity(),
        id -> new SystemSubject(id, this))));
  }
}
