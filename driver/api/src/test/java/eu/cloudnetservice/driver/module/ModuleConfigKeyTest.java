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

package eu.cloudnetservice.driver.module;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModuleConfigKeyTest {

  @Test
  void testModuleIdValidation() {
    var thrownEmpty = Assertions.assertThrows(IllegalArgumentException.class, () -> ModuleConfigKey.of("", "xxxx"));
    Assertions.assertNotNull(thrownEmpty.getMessage());
    Assertions.assertEquals("Module id must not be empty", thrownEmpty.getMessage());

    var thrownSpaces = Assertions.assertThrows(IllegalArgumentException.class, () -> ModuleConfigKey.of("  ", "xxxx"));
    Assertions.assertNotNull(thrownSpaces.getMessage());
    Assertions.assertEquals("Module id must not be empty", thrownSpaces.getMessage());

    Assertions.assertDoesNotThrow(() -> ModuleConfigKey.of("xxxx", "xxxx"));
  }

  @Test
  void testConfigIdValidation() {
    var thrownEmpty = Assertions.assertThrows(IllegalArgumentException.class, () -> ModuleConfigKey.of("xxxx", ""));
    Assertions.assertNotNull(thrownEmpty.getMessage());
    Assertions.assertTrue(thrownEmpty.getMessage().startsWith("Config id \"\" must match pattern \""));

    var thrownSpaces = Assertions.assertThrows(IllegalArgumentException.class, () -> ModuleConfigKey.of("xxxx", "   "));
    Assertions.assertNotNull(thrownSpaces.getMessage());
    Assertions.assertTrue(thrownSpaces.getMessage().startsWith("Config id \"   \" must match pattern \""));

    var thrownSuffixOnly = Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> ModuleConfigKey.of("xxxx", ModuleConfigKey.COMPOSITE_ID_SUFFIX));
    Assertions.assertNotNull(thrownSuffixOnly.getMessage());
    Assertions.assertTrue(thrownSuffixOnly.getMessage().startsWith("Config id \"\" must match pattern \""));

    var thrownInvalidId = Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> ModuleConfigKey.of("xxxx", "in..valid"));
    Assertions.assertNotNull(thrownInvalidId.getMessage());
    Assertions.assertTrue(thrownInvalidId.getMessage().startsWith("Config id \"in..valid\" must match pattern \""));

    var thrownInvalidSuffix = Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> ModuleConfigKey.of("xxxx", "invalid.." + ModuleConfigKey.COMPOSITE_ID_SUFFIX));
    Assertions.assertNotNull(thrownInvalidSuffix.getMessage());
    Assertions.assertTrue(thrownInvalidSuffix.getMessage().startsWith("Config id \"invalid..\" must match pattern \""));
  }

  @Test
  void testSpecificConfigId() {
    var id = ModuleConfigKey.of("xxxx", "specific");
    Assertions.assertEquals("xxxx", id.moduleId());
    Assertions.assertEquals("specific", id.configId());
    Assertions.assertFalse(id.compositeKey());
  }

  @Test
  void testCompositeConfigId() {
    var id = ModuleConfigKey.of("xxxx", "composite_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    Assertions.assertEquals("xxxx", id.moduleId());
    Assertions.assertEquals("composite_", id.configId());
    Assertions.assertTrue(id.compositeKey());
  }

  @Test
  void testDirectCompositeIdConstruction() {
    var id = ModuleConfigKey.ofComposite("xxxx", "composite_");
    Assertions.assertEquals("xxxx", id.moduleId());
    Assertions.assertEquals("composite_", id.configId());
    Assertions.assertTrue(id.compositeKey());
  }

  @Test
  void testSpecificWithModuleId() {
    var id = ModuleConfigKey.of("xxxx", "specific");
    Assertions.assertEquals("xxxx", id.moduleId());
    Assertions.assertEquals("specific", id.configId());
    Assertions.assertFalse(id.compositeKey());

    var withModuleId = id.withModuleId("test");
    Assertions.assertEquals("test", withModuleId.moduleId());
    Assertions.assertEquals("specific", withModuleId.configId());
    Assertions.assertFalse(withModuleId.compositeKey());
  }

  @Test
  void testCompositeWithModuleId() {
    var id = ModuleConfigKey.of("xxxx", "composite_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    Assertions.assertEquals("xxxx", id.moduleId());
    Assertions.assertEquals("composite_", id.configId());
    Assertions.assertTrue(id.compositeKey());

    var withModuleId = id.withModuleId("test");
    Assertions.assertEquals("test", withModuleId.moduleId());
    Assertions.assertEquals("composite_", withModuleId.configId());
    Assertions.assertTrue(withModuleId.compositeKey());
  }

  @Test
  void testSpecificWithConfigId() {
    var id = ModuleConfigKey.of("xxxx", "specific");
    Assertions.assertEquals("xxxx", id.moduleId());
    Assertions.assertEquals("specific", id.configId());
    Assertions.assertFalse(id.compositeKey());

    var withConfigId = id.withConfigId("test");
    Assertions.assertEquals("xxxx", withConfigId.moduleId());
    Assertions.assertEquals("test", withConfigId.configId());
    Assertions.assertFalse(withConfigId.compositeKey());
  }

  @Test
  void testCompositeWithConfigId() {
    var id = ModuleConfigKey.of("xxxx", "composite_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    Assertions.assertEquals("xxxx", id.moduleId());
    Assertions.assertEquals("composite_", id.configId());
    Assertions.assertTrue(id.compositeKey());

    var withConfigId = id.withConfigId("test_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    Assertions.assertEquals("xxxx", withConfigId.moduleId());
    Assertions.assertEquals("test_", withConfigId.configId());
    Assertions.assertTrue(withConfigId.compositeKey());
  }

  @Test
  void testSpecificWitConfigIdToComposite() {
    var id = ModuleConfigKey.of("xxxx", "specific");
    Assertions.assertEquals("xxxx", id.moduleId());
    Assertions.assertEquals("specific", id.configId());
    Assertions.assertFalse(id.compositeKey());

    var withConfigId = id.withConfigId("test_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    Assertions.assertEquals("xxxx", withConfigId.moduleId());
    Assertions.assertEquals("test_", withConfigId.configId());
    Assertions.assertTrue(withConfigId.compositeKey());
  }

  @Test
  void testCompositeWithConfigIdToSpecific() {
    var id = ModuleConfigKey.of("xxxx", "composite_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    Assertions.assertEquals("xxxx", id.moduleId());
    Assertions.assertEquals("composite_", id.configId());
    Assertions.assertTrue(id.compositeKey());

    var withConfigId = id.withConfigId("specific");
    Assertions.assertEquals("xxxx", withConfigId.moduleId());
    Assertions.assertEquals("specific", withConfigId.configId());
    Assertions.assertFalse(withConfigId.compositeKey());
  }

  @Test
  void testNonCompositeWithSuffix() {
    var id = ModuleConfigKey.of("xxxx", "specific");
    Assertions.assertFalse(id.compositeKey());

    var thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> id.withConfigIdSuffix("test"));
    Assertions.assertNotNull(thrown.getMessage());
    Assertions.assertEquals("Cannot add suffix to non-composite config key", thrown.getMessage());
  }

  @Test
  void testCompositeWithSuffixToSpecific() {
    var id = ModuleConfigKey.of("xxxx", "composite_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    Assertions.assertTrue(id.compositeKey());

    var withSuffix = id.withConfigIdSuffix("test");
    Assertions.assertEquals("xxxx", withSuffix.moduleId());
    Assertions.assertEquals("composite_test", withSuffix.configId());
    Assertions.assertFalse(withSuffix.compositeKey());
  }

  @Test
  void testCompositeWithSuffixToComposite() {
    var id = ModuleConfigKey.of("xxxx", "composite_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    Assertions.assertTrue(id.compositeKey());

    var withSuffix = id.withConfigIdSuffix("test_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    Assertions.assertEquals("xxxx", withSuffix.moduleId());
    Assertions.assertEquals("composite_test_", withSuffix.configId());
    Assertions.assertTrue(withSuffix.compositeKey());
  }

  @Test
  void testSpecificJoin() {
    var id = ModuleConfigKey.of("xxxx", "specific");
    Assertions.assertEquals("xxxx/specific", id.join("/"));
    Assertions.assertEquals("xxxx_-_specific", id.join("_-_"));
    Assertions.assertEquals("xxxx/specific", id.joinWithoutCompositeSuffix("/"));
    Assertions.assertEquals("xxxx_-_specific", id.joinWithoutCompositeSuffix("_-_"));
  }

  @Test
  void testCompositeJoin() {
    var id = ModuleConfigKey.of("xxxx", "composite_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    Assertions.assertEquals("xxxx/composite_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX, id.join("/"));
    Assertions.assertEquals("xxxx_-_composite_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX, id.join("_-_"));
    Assertions.assertEquals("xxxx/composite_", id.joinWithoutCompositeSuffix("/"));
    Assertions.assertEquals("xxxx_-_composite_", id.joinWithoutCompositeSuffix("_-_"));
  }

  @Test
  void testSpecificParent() {
    var id = ModuleConfigKey.of("xxxx", "xxxx");
    var same = ModuleConfigKey.of("xxxx", "xxxx");
    Assertions.assertTrue(id.parentOf(same));
    Assertions.assertTrue(id.childOf(same));

    var otherConfigId = id.withConfigId("xxxx_other");
    Assertions.assertFalse(id.parentOf(otherConfigId));
    Assertions.assertFalse(id.childOf(otherConfigId));

    var otherModuleId = id.withModuleId("xxxx_other");
    Assertions.assertFalse(id.parentOf(otherModuleId));
    Assertions.assertFalse(id.childOf(otherModuleId));
  }

  @Test
  void testCompositeParent() {
    var parent = ModuleConfigKey.of("xxxx", "xxxx_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    var same = ModuleConfigKey.of("xxxx", "xxxx_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    Assertions.assertTrue(parent.parentOf(same));
    Assertions.assertTrue(parent.childOf(same));

    var specific = parent.withConfigIdSuffix("test");
    Assertions.assertTrue(parent.parentOf(specific));
    Assertions.assertTrue(specific.childOf(parent));
    Assertions.assertFalse(specific.parentOf(parent));
    Assertions.assertFalse(parent.childOf(specific));

    var composite = parent.withConfigIdSuffix("test_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
    Assertions.assertTrue(parent.parentOf(composite));
    Assertions.assertTrue(composite.childOf(parent));
    Assertions.assertFalse(composite.parentOf(parent));
    Assertions.assertFalse(parent.childOf(composite));
  }

  @Test
  void testSpecificSerialize() {
    try (var buf = DataBuf.empty()) {
      var id = ModuleConfigKey.of("xxxx", "specific");
      buf.writeObject(id);

      var deser = buf.readObject(ModuleConfigKey.class);
      Assertions.assertEquals(id, deser);
    }
  }

  @Test
  void testCompositeSerialize() {
    try (var buf = DataBuf.empty()) {
      var id = ModuleConfigKey.of("xxxx", "composite_" + ModuleConfigKey.COMPOSITE_ID_SUFFIX);
      buf.writeObject(id);

      var deser = buf.readObject(ModuleConfigKey.class);
      Assertions.assertEquals(id, deser);
    }
  }
}
