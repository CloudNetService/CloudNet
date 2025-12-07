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

package eu.cloudnetservice.modules.replacer;

import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.modules.replacer.model.Replacement;
import eu.cloudnetservice.modules.replacer.model.config.Replacements;
import eu.cloudnetservice.modules.replacer.model.config.Replacer;
import eu.cloudnetservice.node.event.service.CloudServicePostPrepareEvent;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ReplacerModule extends DriverModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReplacerModule.class);

  private Replacer configuration;
  private TemplateReplacer replacer;
  private Path replacementsDirectory;

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.STARTED)
  public void handleStart(@NonNull EventManager eventManager) {
    this.reloadInternal();
    eventManager.registerListener(this);
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    this.reloadInternal();
  }

  @EventListener
  public void handle(@NonNull CloudServicePostPrepareEvent event) {
    if (this.replacer == null || this.configuration == null) {
      return;
    }
    this.replacer.apply(event.serviceInfo(), event.service().directory(), null);
  }

  private void reloadInternal() {
    this.configuration = this.readConfig(Replacer.class, ReplacerDefaults::defaultConfiguration, DocumentFactory.json());

    this.replacementsDirectory = ReplacerDefaults.replacementsDirectory(this.moduleWrapper().dataDirectory());
    try {
      Files.createDirectories(this.replacementsDirectory);
    } catch (IOException exception) {
      LOGGER.warn("Unable to create replacements directory {}", this.replacementsDirectory, exception);
    }

    ReplacerDefaults.ensureExampleFiles(this.replacementsDirectory, LOGGER);
    this.replacer = new TemplateReplacer(this.configuration, this.loadRules());
  }

  private List<Replacement> loadRules() {
    if (!Files.isDirectory(this.replacementsDirectory)) {
      return Collections.emptyList();
    }

    var collectedRules = new ArrayList<Replacement>();
    try (var files = Files.walk(this.replacementsDirectory)) {
      files.filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".json"))
        .forEach(path -> this.loadRuleFile(path).forEach(collectedRules::add));
    } catch (IOException exception) {
      LOGGER.warn("Unable to walk replacements directory {}", this.replacementsDirectory, exception);
    }

    return collectedRules;
  }

  private Stream<Replacement> loadRuleFile(Path path) {
    try {
      var document = DocumentFactory.json().parse(path);
      var ruleFile = document.toInstanceOf(Replacements.class);
      if (ruleFile != null && ruleFile.rules() != null) {
        return ruleFile.rules().stream();
      }
    } catch (Exception exception) {
      LOGGER.warn("Unable to parse replacement rule file {}", path, exception);
    }
    return Stream.empty();
  }
}

