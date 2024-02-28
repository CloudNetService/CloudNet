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

package eu.cloudnetservice.modules.cloudperms.sponge.service.memory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

public class InMemorySubjectCollection implements SubjectCollection {

  private final String identifier;
  private final PermissionService service;
  private final Map<String, Subject> subjects;

  public InMemorySubjectCollection(String identifier, PermissionService service) {
    this.identifier = identifier;
    this.service = service;
    this.subjects = new HashMap<>();
  }

  @Override
  public String identifier() {
    return this.identifier;
  }

  @Override
  public Predicate<String> identifierValidityPredicate() {
    return $ -> true;
  }

  @Override
  public CompletableFuture<? extends Subject> loadSubject(String identifier) {
    return CompletableFuture.completedFuture(this.subjects.computeIfAbsent(
      identifier,
      $ -> new InMemorySubject(identifier, this)));
  }

  @Override
  public Optional<? extends Subject> subject(String identifier) {
    return Optional.ofNullable(this.subjects.get(identifier));
  }

  @Override
  public CompletableFuture<Boolean> hasSubject(String identifier) {
    return CompletableFuture.completedFuture(this.subjects.containsKey(identifier));
  }

  @Override
  public CompletableFuture<? extends Map<String, ? extends Subject>> loadSubjects(Iterable<String> identifiers) {
    return CompletableFuture.completedFuture(StreamSupport.stream(identifiers.spliterator(), false)
      .collect(Collectors.toMap(
        Function.identity(),
        id -> this.subjects.computeIfAbsent(id, $ -> new InMemorySubject(id, this)))));
  }

  @Override
  public Collection<? extends Subject> loadedSubjects() {
    return this.subjects.values();
  }

  @Override
  public CompletableFuture<? extends Set<String>> allIdentifiers() {
    return CompletableFuture.completedFuture(this.subjects.keySet());
  }

  @Override
  public SubjectReference newSubjectReference(String subjectIdentifier) {
    return this.service.newSubjectReference(this.identifier, subjectIdentifier);
  }

  @Override
  public CompletableFuture<? extends Map<? extends SubjectReference, Boolean>> allWithPermission(String permission) {
    return CompletableFuture.completedFuture(this.subjects.values().stream()
      .filter(subject -> subject.permissionValue(permission) != Tristate.UNDEFINED)
      .collect(Collectors.toMap(
        Subject::asSubjectReference,
        subject -> subject.permissionValue(permission).asBoolean())));
  }

  @Override
  public CompletableFuture<? extends Map<? extends SubjectReference, Boolean>> allWithPermission(
    String permission,
    Cause cause
  ) {
    return this.allWithPermission(permission);
  }

  @Override
  public Map<? extends Subject, Boolean> loadedWithPermission(String permission) {
    return this.subjects.values().stream()
      .filter(subject -> subject.permissionValue(permission) != Tristate.UNDEFINED)
      .collect(Collectors.toMap(
        Function.identity(),
        subject -> subject.permissionValue(permission).asBoolean()));
  }

  @Override
  public Map<? extends Subject, Boolean> loadedWithPermission(String permission, Cause cause) {
    return this.loadedWithPermission(permission);
  }

  @Override
  public Subject defaults() {
    return this.service.defaults();
  }

  @Override
  public void suggestUnload(String identifier) {
    // no-op
  }
}
