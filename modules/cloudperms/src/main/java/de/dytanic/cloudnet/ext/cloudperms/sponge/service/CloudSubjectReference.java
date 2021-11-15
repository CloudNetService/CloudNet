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

package de.dytanic.cloudnet.ext.cloudperms.sponge.service;

import java.util.concurrent.CompletableFuture;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;

public final class CloudSubjectReference implements SubjectReference {

  private final String collectionId;
  private final String subjectId;

  private final PermissionService permsService;

  public CloudSubjectReference(String collectionId, String subjectId, PermissionService permsService) {
    this.collectionId = collectionId;
    this.subjectId = subjectId;
    this.permsService = permsService;
  }

  @Override
  public String collectionIdentifier() {
    return this.collectionId;
  }

  @Override
  public String subjectIdentifier() {
    return this.subjectId;
  }

  @Override
  public CompletableFuture<? extends Subject> resolve() {
    return this.permsService.loadCollection(this.collectionId).thenCompose(col -> col.loadSubject(this.subjectId));
  }
}
