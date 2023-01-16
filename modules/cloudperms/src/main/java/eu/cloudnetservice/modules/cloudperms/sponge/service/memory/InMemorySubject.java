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

package eu.cloudnetservice.modules.cloudperms.sponge.service.memory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

public class InMemorySubject implements Subject {

  private static final Set<Context> CHECK = Collections.singleton(new Context("cause", "perm_check"));

  private final String identifier;
  private final SubjectCollection owner;
  private final InMemorySubjectData subjectData;

  public InMemorySubject(String identifier, SubjectCollection owner) {
    this.identifier = identifier;
    this.owner = owner;
    this.subjectData = new InMemorySubjectData(this);
  }

  @Override
  public SubjectCollection containingCollection() {
    return this.owner;
  }

  @Override
  public SubjectReference asSubjectReference() {
    return this.owner.newSubjectReference(this.identifier);
  }

  @Override
  public Optional<?> associatedObject() {
    return Optional.empty();
  }

  @Override
  public boolean isSubjectDataPersisted() {
    return false;
  }

  @Override
  public SubjectData subjectData() {
    return this.subjectData;
  }

  @Override
  public SubjectData transientSubjectData() {
    return this.subjectData;
  }

  @Override
  public Tristate permissionValue(String permission, Cause cause) {
    return this.permissionValue(permission, CHECK);
  }

  @Override
  public Tristate permissionValue(String permission, Set<Context> contexts) {
    boolean result = this.subjectData.permissions(contexts).getOrDefault(
      permission,
      this.subjectData.fallbackPermissionValue(contexts).asBoolean());
    return Tristate.fromBoolean(result);
  }

  @Override
  public boolean isChildOf(SubjectReference parent, Cause cause) {
    return false;
  }

  @Override
  public boolean isChildOf(SubjectReference parent, Set<Context> contexts) {
    return false;
  }

  @Override
  public List<? extends SubjectReference> parents(Cause cause) {
    return Collections.emptyList();
  }

  @Override
  public List<? extends SubjectReference> parents(Set<Context> contexts) {
    return Collections.emptyList();
  }

  @Override
  public Optional<String> option(String key, Cause cause) {
    return this.option(key, CHECK);
  }

  @Override
  public Optional<String> option(String key, Set<Context> contexts) {
    return Optional.ofNullable(this.subjectData.options(contexts).get(key));
  }

  @Override
  public String identifier() {
    return this.identifier;
  }
}
