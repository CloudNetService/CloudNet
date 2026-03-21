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

package eu.cloudnetservice.node.impl.module.dependency;

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DefaultModuleDependencyTreeTest {

  @Test
  void testNormalLoadOrder() {
    var bridgeMeta = new ModuleDependencyTestMetadata("bridge", Set.<String>of());
    var npcMeta = new ModuleDependencyTestMetadata("npc", Set.of("bridge"));
    var testMeta = new ModuleDependencyTestMetadata("test", Set.of("bridge", "npc"));

    var dependencyTree = new DefaultModuleDependencyTree();
    dependencyTree.registerModule(npcMeta);
    dependencyTree.registerModule(testMeta);
    dependencyTree.registerModule(bridgeMeta);

    var dependencyCycle = dependencyTree.detectFirstDependencyCycle();
    Assertions.assertTrue(dependencyCycle.isEmpty());

    var loadOrder = dependencyTree.calculateLoadOrder();
    Assertions.assertEquals(3, loadOrder.size());
    Assertions.assertEquals("bridge", loadOrder.getFirst());
    Assertions.assertEquals("npc", loadOrder.get(1));
    Assertions.assertEquals("test", loadOrder.getLast());
  }

  @Test
  void testDetectionOfDependencyCycle() {
    var bridgeMeta = new ModuleDependencyTestMetadata("bridge", Set.of("test"));
    var npcMeta = new ModuleDependencyTestMetadata("npc", Set.of("bridge"));
    var testMeta = new ModuleDependencyTestMetadata("test", Set.of("bridge", "npc"));

    var dependencyTree = new DefaultModuleDependencyTree();
    dependencyTree.registerModule(npcMeta);
    dependencyTree.registerModule(testMeta);
    dependencyTree.registerModule(bridgeMeta);

    var dependencyCycle = dependencyTree.detectFirstDependencyCycle();
    Assertions.assertEquals(4, dependencyCycle.size());
    Assertions.assertEquals("bridge", dependencyCycle.getFirst());
    Assertions.assertEquals("test", dependencyCycle.get(1));
    Assertions.assertEquals("npc", dependencyCycle.get(2));
    Assertions.assertEquals("bridge", dependencyCycle.getLast());
  }

  @Test
  void testModuleDependingOnChecks() {
    var bridgeMeta = new ModuleDependencyTestMetadata("bridge", Set.<String>of());
    var npcMeta = new ModuleDependencyTestMetadata("npc", Set.of("bridge"));
    var testMeta = new ModuleDependencyTestMetadata("test", Set.of("npc"));

    var dependencyTree = new DefaultModuleDependencyTree();
    dependencyTree.registerModule(npcMeta);
    dependencyTree.registerModule(testMeta);
    dependencyTree.registerModule(bridgeMeta);

    var dependencyCycle = dependencyTree.detectFirstDependencyCycle();
    Assertions.assertTrue(dependencyCycle.isEmpty());

    Assertions.assertFalse(dependencyTree.directlyDependingOn(bridgeMeta, npcMeta));
    Assertions.assertFalse(dependencyTree.directlyDependingOn(bridgeMeta, testMeta));
    Assertions.assertFalse(dependencyTree.directlyDependingOn(bridgeMeta, bridgeMeta));
    Assertions.assertFalse(dependencyTree.transitiveDependingOn(bridgeMeta, npcMeta));
    Assertions.assertFalse(dependencyTree.transitiveDependingOn(bridgeMeta, testMeta));
    Assertions.assertFalse(dependencyTree.transitiveDependingOn(bridgeMeta, bridgeMeta));

    Assertions.assertTrue(dependencyTree.directlyDependingOn(npcMeta, bridgeMeta));
    Assertions.assertTrue(dependencyTree.transitiveDependingOn(npcMeta, bridgeMeta));
    Assertions.assertFalse(dependencyTree.directlyDependingOn(npcMeta, testMeta));
    Assertions.assertFalse(dependencyTree.transitiveDependingOn(npcMeta, testMeta));

    Assertions.assertFalse(dependencyTree.directlyDependingOn(testMeta, bridgeMeta));
    Assertions.assertTrue(dependencyTree.transitiveDependingOn(testMeta, bridgeMeta));
    Assertions.assertTrue(dependencyTree.directlyDependingOn(testMeta, npcMeta));
    Assertions.assertTrue(dependencyTree.transitiveDependingOn(testMeta, npcMeta));
  }

  @Test
  void testModuleUnregister() {
    var bridgeMeta = new ModuleDependencyTestMetadata("bridge", Set.<String>of());
    var npcMeta = new ModuleDependencyTestMetadata("npc", Set.of("bridge"));
    var testMeta = new ModuleDependencyTestMetadata("test", Set.of("npc"));

    var dependencyTree = new DefaultModuleDependencyTree();
    dependencyTree.registerModule(npcMeta);
    dependencyTree.registerModule(testMeta);
    dependencyTree.registerModule(bridgeMeta);

    var dependencyCycle = dependencyTree.detectFirstDependencyCycle();
    Assertions.assertTrue(dependencyCycle.isEmpty());

    var unfulfilledDependencies = dependencyTree.unregisterModule(bridgeMeta);
    Assertions.assertEquals(1, unfulfilledDependencies.size());
    Assertions.assertTrue(unfulfilledDependencies.contains("npc"));

    unfulfilledDependencies = dependencyTree.unregisterModule(npcMeta);
    Assertions.assertEquals(1, unfulfilledDependencies.size());
    Assertions.assertTrue(unfulfilledDependencies.contains("test"));

    unfulfilledDependencies = dependencyTree.unregisterModule(testMeta);
    Assertions.assertTrue(unfulfilledDependencies.isEmpty());
    unfulfilledDependencies = dependencyTree.unregisterModule(npcMeta);
    Assertions.assertTrue(unfulfilledDependencies.isEmpty());
    unfulfilledDependencies = dependencyTree.unregisterModule(bridgeMeta);
    Assertions.assertTrue(unfulfilledDependencies.isEmpty());

    var loadOrder = dependencyTree.calculateLoadOrder();
    Assertions.assertTrue(loadOrder.isEmpty());
  }
}
