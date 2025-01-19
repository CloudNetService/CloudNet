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

package eu.cloudnetservice.modules.signs;

import eu.cloudnetservice.modules.bridge.WorldPosition;
import io.leangen.geantyref.TypeFactory;
import java.lang.reflect.Type;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * A sign object representation. It's used for database entries and general handling in the api.
 */
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Sign {

  public static final Type COLLECTION_TYPE = TypeFactory.parameterizedClass(Collection.class, Sign.class);

  protected final String targetGroup;
  protected final String templatePath;

  @EqualsAndHashCode.Include
  protected final WorldPosition worldPosition;

  /**
   * Creates a new sign object
   *
   * @param targetGroup   the group the sign is targeting
   * @param worldPosition the position of the sign in the world
   */
  public Sign(@NonNull String targetGroup, @NonNull WorldPosition worldPosition) {
    this(targetGroup, null, worldPosition);
  }

  /**
   * Creates a new sign object
   *
   * @param targetGroup   the group the sign is targeting
   * @param templatePath  the template of this
   * @param worldPosition the position of the sign in the world
   */
  public Sign(@NonNull String targetGroup, @Nullable String templatePath, @NonNull WorldPosition worldPosition) {
    this.targetGroup = targetGroup;
    this.templatePath = templatePath;
    this.worldPosition = worldPosition;
  }

  public @NonNull String targetGroup() {
    return this.targetGroup;
  }

  public @Nullable String templatePath() {
    return this.templatePath;
  }

  public @NonNull WorldPosition location() {
    return this.worldPosition;
  }
}
