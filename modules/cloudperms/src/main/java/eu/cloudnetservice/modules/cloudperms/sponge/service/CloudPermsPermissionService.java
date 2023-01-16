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

package eu.cloudnetservice.modules.cloudperms.sponge.service;

import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.modules.cloudperms.sponge.service.memory.InMemorySubjectCollection;
import eu.cloudnetservice.modules.cloudperms.sponge.service.permissible.group.CloudGroupCollection;
import eu.cloudnetservice.modules.cloudperms.sponge.service.permissible.user.CloudUserCollection;
import eu.cloudnetservice.modules.cloudperms.sponge.service.system.SystemSubjectCollection;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import lombok.NonNull;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.plugin.PluginContainer;

public final class CloudPermsPermissionService implements PermissionService {

  final Map<String, SubjectCollection> collections = new HashMap<>();
  final Collection<PermissionDescription> descriptions = new HashSet<>();

  private final Subject defaultSubject;

  public CloudPermsPermissionService(@NonNull PermissionManagement management) {
    this.collections.put(SUBJECTS_USER, new CloudUserCollection(SUBJECTS_USER, this, management));
    this.collections.put(SUBJECTS_GROUP, new CloudGroupCollection(SUBJECTS_GROUP, this, management));
    this.collections.put(SUBJECTS_SYSTEM, new SystemSubjectCollection(SUBJECTS_SYSTEM, this));

    this.collections.put(SUBJECTS_DEFAULT, new InMemorySubjectCollection(SUBJECTS_DEFAULT, this));
    this.defaultSubject = this.collections.get(SUBJECTS_DEFAULT).loadSubject("default").join();
  }

  @Override
  public SubjectCollection userSubjects() {
    return this.collections.get(SUBJECTS_USER);
  }

  @Override
  public SubjectCollection groupSubjects() {
    return this.collections.get(SUBJECTS_GROUP);
  }

  @Override
  public Subject defaults() {
    return this.defaultSubject;
  }

  @Override
  public Predicate<String> identifierValidityPredicate() {
    return $ -> true;
  }

  @Override
  public CompletableFuture<? extends SubjectCollection> loadCollection(String identifier) {
    // get if loaded
    var collection = this.collections.get(identifier);
    if (collection != null) {
      return CompletableFuture.completedFuture(collection);
    }
    // create a new in-memory collection
    collection = new InMemorySubjectCollection(identifier, this);
    this.collections.put(identifier, collection);
    // return that one
    return CompletableFuture.completedFuture(collection);
  }

  @Override
  public Optional<? extends SubjectCollection> collection(String identifier) {
    return Optional.ofNullable(this.collections.get(identifier));
  }

  @Override
  public CompletableFuture<Boolean> hasCollection(String identifier) {
    return CompletableFuture.completedFuture(this.collection(identifier).isPresent());
  }

  @Override
  public Map<String, ? extends SubjectCollection> loadedCollections() {
    return this.collections;
  }

  @Override
  public CompletableFuture<? extends Set<String>> allIdentifiers() {
    return CompletableFuture.completedFuture(this.collections.keySet());
  }

  @Override
  public SubjectReference newSubjectReference(String collectionIdentifier, String subjectIdentifier) {
    return new CloudSubjectReference(collectionIdentifier, subjectIdentifier, this);
  }

  @Override
  public PermissionDescription.Builder newDescriptionBuilder(PluginContainer plugin) {
    return new CloudPermissionDescription.CloudPermissionDescriptionBuilder(plugin, this);
  }

  @Override
  public Optional<? extends PermissionDescription> description(String permission) {
    return this.descriptions.stream().filter(desc -> desc.id().equals(permission)).findFirst();
  }

  @Override
  public Collection<? extends PermissionDescription> descriptions() {
    return this.descriptions;
  }
}
