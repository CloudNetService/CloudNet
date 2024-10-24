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

package eu.cloudnetservice.driver.template;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The main api point to work with template storages. This api does not support registering or unregistering template
 * storages. Storages are registered on each node using the local present service registry and are only accessed by that
 * node. Remote api components like the wrapper can only access them if they know the name of them, but are not able to
 * define own storages in any way. Furthermore, this means that storages might only be present on some nodes which are
 * running in the cluster and not on all nodes.
 *
 * @since 4.0
 */
public interface TemplateStorageProvider {

  /**
   * Returns the local template storage of the current node. This template storage is special as it is always present by
   * default and writes all the data of a template to the local file system. This method does only throw an exception
   * when accessing the storage if a module forcefully removed the local template storage from the node.
   * <p>
   * Because this method is supposed to return a wrapper object on remote api components there is no need for an async
   * variant of this method.
   *
   * @return the local template storage.
   * @throws UnsupportedOperationException if the local template storage was unregistered.
   */
  @NonNull
  TemplateStorage localTemplateStorage();

  /**
   * Get the template storage with the given name, this method returns null if no storage with that name is registered.
   * Remote api components (like the wrapper) might always return a non-null value from this method, failing if methods
   * are invoked remotely (on the associated node) on the returned storage object.
   * <p>
   * The returned storage object is not stateless meaning that the storage might get closed and unregistered. Keeping an
   * instance of a template storage object on non-remote api components is therefore not safe and should be avoided.
   * <p>
   * Because this method is supposed to return a wrapper object on remote api components there is no need for an async
   * variant of this method.
   *
   * @param storage the name of the storage to get.
   * @return the template storage with the given name, null if the storage is unknown.
   * @throws NullPointerException if the given storage name is null.
   */
  @Nullable TemplateStorage templateStorage(@NonNull String storage);

  /**
   * Get the names of all template storages which are currently registered on the associated node. This collection is
   * not safe to cache as it contents might change without a reflection into the returned collection.
   * <p>
   * Obtaining a template storage object based on the names of the template storages which are returned by this method
   * can still evaluate in an unknown template storage.
   *
   * @return the names of all template storages which are currently registered.
   */
  @NonNull
  Collection<String> availableTemplateStorages();

  /**
   * Get the names of all template storages which are currently registered on the associated node. This collection is
   * not safe to cache as it contents might change without a reflection into the returned collection.
   * <p>
   * Obtaining a template storage object based on the names of the template storages which are returned by this method
   * can still evaluate in an unknown template storage.
   *
   * @return a task completed with the names of all template storages which are currently registered.
   */
  @NonNull
  CompletableFuture<Collection<String>> availableTemplateStoragesAsync();
}
