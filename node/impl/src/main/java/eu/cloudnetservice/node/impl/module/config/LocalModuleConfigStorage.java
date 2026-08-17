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

package eu.cloudnetservice.node.impl.module.config;

import eu.cloudnetservice.driver.base.DisposableResource;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.module.ModuleConfigKey;
import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorage;
import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorageDescriptor;
import eu.cloudnetservice.node.module.config.storage.ModuleConfigStorageIOException;
import eu.cloudnetservice.node.module.config.storage.StandardModuleConfigStorageFlag;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of a module config storage that stores on the local file system.
 *
 * @since 4.0
 */
public final class LocalModuleConfigStorage implements ModuleConfigStorage {

  public static final String CONFIG_FILE_SUFFIX = ".conf";
  public static final String LOCAL_MODULE_CONFIG_STORAGE = "local";
  public static final ModuleConfigStorageDescriptor STORAGE_DESCRIPTOR = new DefaultModuleConfigStorageDescriptor(
    LOCAL_MODULE_CONFIG_STORAGE,
    List.of(
      StandardModuleConfigStorageFlag.SUPPORTS_STORING,
      StandardModuleConfigStorageFlag.SUPPORTS_DELETION,
      StandardModuleConfigStorageFlag.SUPPORTS_WATCHING));

  private final Path storageDirectory;
  private final WatchService watchService;

  private final Map<WatchKey, Path> watchedDirectories = new ConcurrentHashMap<>();
  private final Set<WatcherRegistration> watcherRegistrations = ConcurrentHashMap.newKeySet();

  public LocalModuleConfigStorage(@NonNull Path storageDirectory) {
    try {
      this.storageDirectory = storageDirectory;
      this.watchService = storageDirectory.getFileSystem().newWatchService();
      this.registerDirectoryWatcher(storageDirectory);
      this.availableModuleDirectories().forEach(this::registerDirectoryWatcher);
      Thread.ofVirtual().name("local-module-config-watcher").start(this::processWatchEvents);
    } catch (IOException exception) {
      throw new ModuleConfigStorageIOException("Unable to initialize local module config watcher", exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return LOCAL_MODULE_CONFIG_STORAGE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ModuleConfigStorageDescriptor descriptor() {
    return STORAGE_DESCRIPTOR;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Set<ModuleConfigKey> availableConfigKeys() {
    return this.availableModuleDirectories()
      .flatMap(this::availableConfigFiles)
      .filter(Files::isRegularFile)
      .map(this::keyFromPath)
      .filter(Objects::nonNull)
      .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Set<ModuleConfigKey> availableConfigKeys(@NonNull ModuleConfigKey key) {
    return this.availableConfigKeys().stream()
      .filter(availableKey -> this.keyMatches(key, availableKey))
      .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable InputStream loadConfig(@NonNull ModuleConfigKey key) {
    var path = this.pathForKey(key);
    if (Files.notExists(path) || Files.isDirectory(path)) {
      return null;
    }

    try {
      return Files.newInputStream(path);
    } catch (IOException exception) {
      throw new ModuleConfigStorageIOException("Unable to load config " + key, exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void storeConfig(@NonNull ModuleConfigKey key, @NonNull Document document) {
    try {
      var path = this.pathForKey(key);
      Files.createDirectories(path.getParent());
      document.writeTo(path);
      this.registerDirectoryWatcher(path.getParent());
    } catch (IOException exception) {
      throw new ModuleConfigStorageIOException("Unable to store config " + key, exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteConfig(@NonNull ModuleConfigKey key) {
    try {
      Files.deleteIfExists(this.pathForKey(key));
    } catch (IOException exception) {
      throw new ModuleConfigStorageIOException("Unable to delete config " + key, exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DisposableResource watchForUpdates(
    @Nullable ModuleConfigKey key,
    @NonNull Consumer<ModuleConfigKey> updateListener
  ) {
    var registration = new WatcherRegistration(key, updateListener);
    this.watcherRegistrations.add(registration);
    return () -> this.watcherRegistrations.remove(registration);
  }

  /**
   * Lists all module directories that are currently present in the storage directory.
   *
   * @return all available module directories in this storage.
   */
  private @NonNull Stream<Path> availableModuleDirectories() {
    try (var directoryStream = Files.list(this.storageDirectory)) {
      return directoryStream.filter(Files::isDirectory).toList().stream();
    } catch (IOException exception) {
      throw new ModuleConfigStorageIOException("Unable to list module config directories", exception);
    }
  }

  /**
   * Lists all config files that are currently present in the given module directory.
   *
   * @param moduleDirectory the module directory to list the config files of.
   * @return all available config files in the given module directory.
   */
  private @NonNull Stream<Path> availableConfigFiles(@NonNull Path moduleDirectory) {
    try (var directoryStream = Files.list(moduleDirectory)) {
      return directoryStream.toList().stream();
    } catch (IOException exception) {
      throw new ModuleConfigStorageIOException("Unable to list configs in " + moduleDirectory, exception);
    }
  }

  /**
   * Constructs a module config key from the given path if the path targets a config file in a module directory.
   *
   * @param path the path to construct the module config key from.
   * @return the module config key represented by the path or null if the path does not target a config file.
   */
  private @Nullable ModuleConfigKey keyFromPath(@NonNull Path path) {
    var relativePath = this.storageDirectory.relativize(path);
    if (relativePath.getNameCount() != 2) {
      return null;
    }

    var moduleId = relativePath.getName(0).toString();
    var rawFileName = relativePath.getName(1).toString();
    if (!rawFileName.endsWith(CONFIG_FILE_SUFFIX)) {
      return null;
    }

    var configId = rawFileName.substring(0, rawFileName.length() - CONFIG_FILE_SUFFIX.length());
    return ModuleConfigKey.of(moduleId, configId);
  }

  /**
   * Constructs the local config file path for the given module config key.
   *
   * @param key the key to construct the local config file path for.
   * @return the local config file path for the given module config key.
   */
  private @NonNull Path pathForKey(@NonNull ModuleConfigKey key) {
    return this.storageDirectory.resolve(key.moduleId()).resolve(key.configId() + CONFIG_FILE_SUFFIX);
  }

  /**
   * Checks if the given candidate key matches the given filter key.
   *
   * @param filter    the key to use as a filter.
   * @param candidate the candidate key to check.
   * @return true if the candidate key matches the filter key, false otherwise.
   */
  private boolean keyMatches(@NonNull ModuleConfigKey filter, @NonNull ModuleConfigKey candidate) {
    if (!filter.moduleId().equals(candidate.moduleId())) {
      return false;
    }

    return filter.compositeKey()
      ? candidate.configId().startsWith(filter.configId())
      : candidate.configId().equals(filter.configId());
  }

  /**
   * Registers the given directory for change watching if it is not already watched.
   *
   * @param directory the directory to register for change watching.
   */
  private void registerDirectoryWatcher(@NonNull Path directory) {
    if (this.watchedDirectories.containsValue(directory)) {
      return;
    }

    try {
      var watchKey = directory.register(
        this.watchService,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_DELETE);
      this.watchedDirectories.put(watchKey, directory);
    } catch (IOException exception) {
      throw new ModuleConfigStorageIOException("Unable to watch module config directory " + directory, exception);
    }
  }

  /**
   * Processes all events emitted by the watch service until the watcher thread is interrupted.
   */
  private void processWatchEvents() {
    var currentThread = Thread.currentThread();
    try {
      while (!currentThread.isInterrupted()) {
        // take the next watch key event and process it
        var watchKey = this.watchService.take();
        var directory = this.watchedDirectories.get(watchKey);
        if (directory != null) {
          this.handleWatchKey(watchKey, directory);
        }

        // try to reset the watch key, unregister it if it is no longer valid
        if (!watchKey.reset()) {
          this.watchedDirectories.remove(watchKey);
        }
      }
    } catch (ClosedWatchServiceException _) {
      // ignored, watch service was closed
    } catch (InterruptedException _) {
      currentThread.interrupt(); // reset interrupted state
    }
  }

  /**
   * Handles all pending events of the given watch key and notifies registered listeners about matching config changes.
   *
   * @param watchKey  the watch key that emitted the events.
   * @param directory the watched directory in which the events occurred.
   */
  private void handleWatchKey(@NonNull WatchKey watchKey, @NonNull Path directory) {
    for (var event : watchKey.pollEvents()) {
      if (!(event.context() instanceof Path changedFileName)) {
        continue;
      }

      var changedPath = directory.resolve(changedFileName);
      if (Files.isDirectory(changedPath)) {
        this.registerDirectoryWatcher(changedPath);
        continue;
      }

      var key = this.keyFromPath(changedPath);
      if (key != null) {
        this.watcherRegistrations.stream()
          .filter(registration -> registration.key == null || this.keyMatches(registration.key, key))
          .forEach(registration -> registration.listener.accept(key));
      }
    }
  }

  /**
   * Registration of a watcher listener and the optional key filter applied to events before notification.
   *
   * @param key      an optional key filter for update notifications.
   * @param listener the listener to notify about matching config changes.
   * @since 4.0
   */
  private record WatcherRegistration(@Nullable ModuleConfigKey key, @NonNull Consumer<ModuleConfigKey> listener) {

  }
}
