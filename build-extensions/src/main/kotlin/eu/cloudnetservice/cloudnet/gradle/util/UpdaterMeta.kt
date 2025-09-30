/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.gradle.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.io.Serializable

data class UpdaterMeta(
  val type: Type,
  val data: Data
) : Serializable {
  companion object {
    val gson: Gson = GsonBuilder()
      .serializeNulls()
      .disableHtmlEscaping()
      .registerTypeHierarchyAdapter(UpdaterMeta::class.java, MetaAdapter)
      .create()
  }

  /**
   * Custom serializer to support the Data interface
   */
  object MetaAdapter : JsonSerializer<UpdaterMeta>, JsonDeserializer<UpdaterMeta> {
    override fun serialize(
      src: UpdaterMeta,
      typeOfSrc: java.lang.reflect.Type,
      context: JsonSerializationContext
    ): JsonElement {
      val json = JsonObject()
      json.add("type", context.serialize(src.type))
      json.add("data", context.serialize(src.data))
      return json
    }

    override fun deserialize(
      json: JsonElement,
      typeOfT: java.lang.reflect.Type,
      context: JsonDeserializationContext
    ): UpdaterMeta {
      val json = json.asJsonObject
      val type = context.deserialize<Type>(json.get("type"), Type::class.java)
      val dataClass = when (type) {
        Type.MODULE -> Data.Module::class
        Type.NODE -> Data.Node::class
        Type.LAUNCHER -> Data.Launcher::class
        Type.LAUNCHER_PATCHER -> Data.LauncherPatcher::class
        Type.EMPTY -> Data.Empty::class
      }
      val data = context.deserialize<Data>(json.get("data"), dataClass.java)
      return UpdaterMeta(type, data)
    }
  }

  enum class Type : Serializable {
    MODULE,
    NODE,
    LAUNCHER,
    LAUNCHER_PATCHER,

    /**
     * Empty type. Should in reality never be used and will print a warning when detected.
     * This is useful because it allows for nice conventions and better initial setup/troubleshooting.
     */
    EMPTY
  }

  sealed interface Data : Serializable {
    data class Module(
      val archiveName: String,
      val moduleJsonName: String
    ) : Data

    data class Node(
      val archiveName: String,
    ) : Data

    data class Launcher(
      val archiveName: String
    ) : Data

    data class LauncherPatcher(
      val archiveName: String
    ) : Data

    object Empty : Data {
      @Suppress("unused") // kotlin wants this
      private fun readResolve(): Any = Empty
    }
  }
}
