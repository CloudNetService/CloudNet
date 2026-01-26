/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.event.service;

import eu.cloudnetservice.node.service.CloudService;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * An event which is called after the service class path got constructed, but before it is being used to start the
 * service. Removing elements from the class path is not possible.
 *
 * @since 4.0
 */
public final class CloudServiceJvmClassPathConstructEvent extends CloudServiceEvent {

  private final Collection<Path> classPath;

  /**
   * Constructs a new cloud service jvm class path event.
   *
   * @param service   the service for which the class path got constructed.
   * @param classPath the constructed class path.
   */
  public CloudServiceJvmClassPathConstructEvent(@NonNull CloudService service, @NonNull Collection<Path> classPath) {
    super(service);
    this.classPath = classPath;
  }

  /**
   * Gets the constructed class path for the service. This set is an unmodifiable view of the class path.
   *
   * @return the constructed class path.
   */
  @UnmodifiableView
  public @NonNull Collection<Path> classPath() {
    return Collections.unmodifiableCollection(this.classPath);
  }

  /**
   * Adds a new entry to the class path of the service.
   *
   * @param path the path to add.
   * @throws NullPointerException if the given path is null.
   */
  public void addClassPathEntry(@NonNull Path path) {
    this.classPath.add(path);
  }

  /**
   * Adds multiple new entries to the class path of the service.
   *
   * @param paths the paths to add.
   * @throws NullPointerException if the given collection is null.
   */
  public void addClassPathEntries(@NonNull Collection<Path> paths) {
    this.classPath.addAll(paths);
  }
}
