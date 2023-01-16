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

package eu.cloudnetservice.modules.cloudperms.sponge.service.system;

import eu.cloudnetservice.modules.cloudperms.sponge.service.memory.InMemorySubject;
import java.util.Set;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.util.Tristate;

public final class SystemSubject extends InMemorySubject implements org.spongepowered.api.SystemSubject {

  public SystemSubject(String identifier, SubjectCollection owner) {
    super(identifier, owner);
  }

  @Override
  public Tristate permissionValue(String permission, Set<Context> contexts) {
    return Tristate.TRUE;
  }
}
