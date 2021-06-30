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

package de.dytanic.cloudnet.common.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.TypeAdapters;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.gson.JsonDocumentTypeAdapter;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

/**
 * Includes a Gson object as member, which should protect for multiple creation of Gson instances
 *
 * @see Gson
 */
@Deprecated
@ScheduledForRemoval(inVersion = "3.6")
public final class GsonUtil {

  /**
   * The Gson constant instance, which should use as a new Gson object instance The following attributes has with
   * GsonBuilder.
   * <p>
   * The serializer has no pretty printing. You can use new JsonDocument(obj).toPrettyJson(); als alternative
   *
   * @see Gson
   * @see GsonBuilder
   * <p>
   * serializeNulls disableHtmlEscaping
   */
  public static final Gson GSON = new GsonBuilder()
    .serializeNulls()
    .disableHtmlEscaping()
    .registerTypeAdapterFactory(TypeAdapters.newTypeHierarchyFactory(JsonDocument.class, new JsonDocumentTypeAdapter()))
    .create();

  private GsonUtil() {
    throw new UnsupportedOperationException();
  }
}
