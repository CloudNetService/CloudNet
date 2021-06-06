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

package de.dytanic.cloudnet.launcher.test;

import de.dytanic.cloudnet.launcher.cnl.CNLInterpreter;
import de.dytanic.cloudnet.launcher.cnl.defaults.CNLCommandCNL;
import de.dytanic.cloudnet.launcher.cnl.defaults.CNLCommandEcho;
import de.dytanic.cloudnet.launcher.cnl.defaults.CNLCommandVar;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class CNLInterpreterTest {

  @Test
  public void testInterpreter() throws Throwable {
    Map<String, String> variables = new HashMap<>();

    CNLInterpreter.registerCommand(new CNLCommandVar());
    CNLInterpreter.registerCommand(new CNLCommandEcho());
    CNLInterpreter.registerCommand(new CNLCommandCNL());

    CNLInterpreter.runInterpreter(CNLInterpreter.class.getClassLoader().getResourceAsStream("test.cnl"), variables);

    Assert.assertEquals(3, variables.size());
    Assert.assertTrue(variables.containsKey("test_val"));
    Assert.assertTrue(variables.containsKey("val"));
    Assert.assertTrue(variables.containsKey("signature_value"));
    Assert.assertEquals("This is our signature", variables.get("signature_value"));
  }
}
