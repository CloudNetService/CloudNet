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

    CNLInterpreter.runInterpreter(
        CNLInterpreter.class.getClassLoader().getResourceAsStream("test.cnl"),
        variables);

    Assert.assertEquals(3, variables.size());
    Assert.assertTrue(variables.containsKey("test_val"));
    Assert.assertTrue(variables.containsKey("val"));
    Assert.assertTrue(variables.containsKey("signature_value"));
    Assert.assertEquals("This is our signature",
        variables.get("signature_value"));
  }
}