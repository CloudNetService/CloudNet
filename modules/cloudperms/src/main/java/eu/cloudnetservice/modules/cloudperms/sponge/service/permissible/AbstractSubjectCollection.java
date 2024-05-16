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

package eu.cloudnetservice.modules.cloudperms.sponge.service.permissible;

import eu.cloudnetservice.driver.permission.PermissionManagement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectReference;

public abstract class AbstractSubjectCollection implements SubjectCollection {

  protected final String identifier;
  protected final PermissionService permsService;
  protected final PermissionManagement management;

  public AbstractSubjectCollection(String identifier, PermissionService service, PermissionManagement management) {
    this.identifier = identifier;
    this.permsService = service;
    this.management = management;
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
  public CompletableFuture<? extends Map<String, ? extends Subject>> loadSubjects(Iterable<String> identifiers) {
    return CompletableFuture.supplyAsync(() -> {
      Map<String, Subject> result = new HashMap<>();
      // load only one subject at a time
      for (var s : identifiers) {
        var subject = this.loadSubject(s).join();
        if (subject != null) {
          result.put(s, subject);
        }
      }
      return result;
    });
  }

  @Override
  public SubjectReference newSubjectReference(String subjectIdentifier) {
    return this.permsService.newSubjectReference(this.identifier, subjectIdentifier);
  }

  @Override
  public CompletableFuture<? extends Map<? extends SubjectReference, Boolean>> allWithPermission(String permission) {
    return this.allWithPermission(permission, null);
  }

  @Override
  public Map<? extends Subject, Boolean> loadedWithPermission(String permission) {
    return this.loadedWithPermission(permission, null);
  }

  @Override
  public Subject defaults() {
    throw new UnsupportedOperationException("Defaults are not supported for this collection");
  }

  @Override
  public void suggestUnload(String identifier) {
    // no-op
  }
}
