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

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceId;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.ThreadSnapshot;
import eu.cloudnetservice.modules.replacer.model.PlaceholderReplacement;
import eu.cloudnetservice.modules.replacer.model.Replacement;
import eu.cloudnetservice.modules.replacer.model.TargetDefinition;
import eu.cloudnetservice.modules.replacer.model.condition.ConditionRule;
import eu.cloudnetservice.modules.replacer.model.condition.ConditionWhen;
import eu.cloudnetservice.modules.replacer.model.config.Replacer;
import eu.cloudnetservice.modules.replacer.type.ReplaceType;
import eu.cloudnetservice.modules.replacer.type.SearchType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class TemplateReplacerTest {

  @TempDir
  private Path tempDir;

  private ServiceInfoSnapshot service;

  @BeforeEach
  void setup() {
    this.service = this.mockService(
      "Node-1",
      "Lobby",
      "Lobby-1",
      "MINECRAFT_SERVER",
      "127.0.0.1",
      25565);
  }

  @Test
  void testSequentialWrappingAllMatches() throws Exception {
    var file = this.write("config.yml", "A %token% B %token% C %token%");

    var rule = new Replacement(
      "seq",
      true,
      List.of(new TargetDefinition("Lobby", null, null, null, null)),
      List.of("config.yml"),
      List.of(new PlaceholderReplacement(
        "%token%",
        SearchType.ALL,
        ReplaceType.SEQUENTIAL,
        List.of("X", "Y"),
        null)));

    var replacer = this.replacerWithRules(List.of(rule));
    replacer.apply(this.service, this.tempDir, null);

    assertEquals("A X B Y C X", Files.readString(file, StandardCharsets.UTF_8));
  }

  @Test
  void testConditionalReplacement() throws Exception {
    var file = this.write("config.txt", "%token% waiting");

    var rule = new Replacement(
      "conditional",
      true,
      List.of(new TargetDefinition(null, null, "MINECRAFT_SERVER", null, null)),
      List.of("config.txt"),
      List.of(new PlaceholderReplacement(
        "%token%",
        SearchType.FIRST,
        ReplaceType.CONDITIONAL,
        null,
        List.of(
          new ConditionRule(new ConditionWhen("environment", "MINECRAFT_SERVER", null), "bridge"),
          new ConditionRule(new ConditionWhen("environment", "BUNGEECORD", null), "proxy")))));

    var replacer = this.replacerWithRules(List.of(rule));
    replacer.apply(this.service, this.tempDir, null);

    assertEquals("bridge waiting", Files.readString(file, StandardCharsets.UTF_8));
  }

  @Test
  void testBuiltInPlaceholdersApplied() throws Exception {
    var file = this.write("config.txt", "%taskName% %serviceName% %nodeId% %serviceHost% %servicePort%");

    var replacer = this.replacerWithRules(List.of());
    replacer.apply(this.service, this.tempDir, null);

    assertEquals("Lobby Lobby-1 Node-1 127.0.0.1 25565", Files.readString(file, StandardCharsets.UTF_8));
  }

  @Test
  void testDisabledRuleSkipped() throws Exception {
    var file = this.write("config.txt", "value %token%");

    var rule = new Replacement(
      "disabled",
      false,
      List.of(new TargetDefinition("Lobby", null, null, null, null)),
      List.of("config.txt"),
      List.of(new PlaceholderReplacement("%token%", SearchType.ALL, ReplaceType.FIRST, List.of("X"), null)));

    var replacer = this.replacerWithRules(List.of(rule));
    replacer.apply(this.service, this.tempDir, null);

    assertEquals("value %token%", Files.readString(file, StandardCharsets.UTF_8));
  }

  @Test
  void testDefaultFileGlobsUsedWhenRuleMissingFiles() throws Exception {
    var file = this.write("a.yml", "%token%");

    var rule = new Replacement(
      "glob-default",
      true,
      List.of(new TargetDefinition("Lobby", null, null, null, null)),
      null,
      List.of(new PlaceholderReplacement("%token%", SearchType.ALL, ReplaceType.FIRST, List.of("X"), null)));

    var replacer = this.replacerWithRules(List.of(rule));
    replacer.apply(this.service, this.tempDir, null);

    assertEquals("X", Files.readString(file, StandardCharsets.UTF_8));
  }

  @Test
  void testGroupTargetMatches() throws Exception {
    var file = this.write("group.txt", "%token%");

    var rule = new Replacement(
      "group-target",
      true,
      List.of(new TargetDefinition(null, null, null, "LobbyGroup", null)),
      List.of("group.txt"),
      List.of(new PlaceholderReplacement("%token%", SearchType.ALL, ReplaceType.FIRST, List.of("G"), null)));

    var replacer = this.replacerWithRules(List.of(rule));
    replacer.apply(this.service, this.tempDir, null);

    assertEquals("G", Files.readString(file, StandardCharsets.UTF_8));
  }

  private TemplateReplacer replacerWithRules(List<Replacement> rules) {
    var config = new Replacer(
      true,
      new Replacer.DefaultSection(SearchType.ALL, ReplaceType.FIRST),
      new Replacer.PathSection(List.of(
        "**/*.yml",
        "**/*.txt")),
      new Replacer.LimitSection(524_288));
    return new TemplateReplacer(config, rules);
  }

  private Path write(String name, String content) throws IOException {
    var file = this.tempDir.resolve(name);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file;
  }

  private ServiceInfoSnapshot mockService(
    String nodeId,
    String task,
    String serviceName,
    String environment,
    String host,
    int port
  ) {
    var serviceId = Mockito.mock(ServiceId.class);
    Mockito.when(serviceId.taskName()).thenReturn(task);
    Mockito.when(serviceId.name()).thenReturn(serviceName);
    Mockito.when(serviceId.environmentName()).thenReturn(environment);
    Mockito.when(serviceId.nodeUniqueId()).thenReturn(nodeId);

    var configuration = Mockito.mock(ServiceConfiguration.class);
    Mockito.when(configuration.serviceId()).thenReturn(serviceId);
    Mockito.when(configuration.port()).thenReturn(port);
    Mockito.when(configuration.groups()).thenReturn(Set.of("LobbyGroup"));

    var serviceInfo = Mockito.mock(ServiceInfoSnapshot.class);
    Mockito.when(serviceInfo.serviceId()).thenReturn(serviceId);
    Mockito.when(serviceInfo.configuration()).thenReturn(configuration);
    Mockito.when(serviceInfo.address()).thenReturn(new HostAndPort(host, port));
    Mockito.when(serviceInfo.lifeCycle()).thenReturn(ServiceLifeCycle.PREPARED);
    Mockito.when(serviceInfo.processSnapshot()).thenReturn(new eu.cloudnetservice.driver.service.ProcessSnapshot(
      0, 0, 0, 0, 0, 0, 0, 0, 0, List.of(ThreadSnapshot.from(Thread.currentThread()))));

    // fallback for id -> config requires a task
    Mockito.when(serviceId.environment()).thenReturn(null);
    return serviceInfo;
  }
}

