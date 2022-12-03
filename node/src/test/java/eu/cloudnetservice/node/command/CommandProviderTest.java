/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.node.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.NoSuchCommandException;
import eu.cloudnetservice.driver.NodeTestUtility;
import eu.cloudnetservice.driver.event.DefaultEventManager;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.defaults.DefaultCommandProvider;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.command.source.DriverCommandSource;
import eu.cloudnetservice.node.console.Console;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Iterables;

public final class CommandProviderTest {
/*
  private static final CommandProvider COMMAND_PROVIDER = new DefaultCommandProvider(Mockito.mock(Console.class),
    new DefaultEventManager());

  @BeforeAll
  public static void initNode() {
    var node = NodeTestUtility.mockAndSetDriverInstance();
    Mockito.when(node.commandProvider()).thenReturn(COMMAND_PROVIDER);
    // register two test commands
    COMMAND_PROVIDER.register(new TestCommand());
    COMMAND_PROVIDER.register(new HelpTestCommand());
  }

  @Test
  public void testCommandRegistration() {
    var testCommand = COMMAND_PROVIDER.command("tests");
    Assertions.assertNotNull(testCommand);
    Assertions.assertEquals(1, testCommand.usage().size());
    Assertions.assertEquals("tests test <user>", Iterables.firstOf(testCommand.usage()));

    var testCommandByAlias = COMMAND_PROVIDER.command("test1");
    Assertions.assertNotNull(testCommandByAlias);
    Assertions.assertNotEquals("test1", testCommand.name());
    Assertions.assertEquals(testCommandByAlias, testCommandByAlias);
  }

  @Test
  public void testStaticCommandSuggestions() {
    var source = new DriverCommandSource();

    var rootSuggestions = COMMAND_PROVIDER.suggest(source, "tests");
    // confirm is registered by default
    Assertions.assertEquals(3, rootSuggestions.size());
    Assertions.assertEquals(Arrays.asList("help", "tests", "confirm"), rootSuggestions);

    var subSuggestions = COMMAND_PROVIDER.suggest(source, "tests ");
    Assertions.assertEquals(1, subSuggestions.size());
    Assertions.assertEquals("test", Iterables.firstOf(subSuggestions));
  }

  @Test
  public void testDynamicCommandSuggestions() {
    var source = new DriverCommandSource();

    var suggestions = COMMAND_PROVIDER.suggest(source, "tests test ");
    Assertions.assertEquals(3, suggestions.size());
    Assertions.assertEquals(Arrays.asList("alice", "bob", "clyde"), suggestions);
  }

  @Test
  public void testCommandNotFound() {
    var source = new DriverCommandSource();

    try {
      COMMAND_PROVIDER.execute(source, "non existing command").getOrNull();
    } catch (CompletionException exception) {
      Assertions.assertEquals(NoSuchCommandException.class, exception.getCause().getClass());
    }
  }

  @Test
  public void testCommandUnregister() {
    // the confirmation command is always registered so there are 3 commands
    Assertions.assertEquals(3, COMMAND_PROVIDER.commands().size());
    COMMAND_PROVIDER.unregister(this.getClass().getClassLoader());
    Assertions.assertEquals(0, COMMAND_PROVIDER.commands().size());
  }

  public static final class HelpTestCommand {

    @CommandMethod("help")
    public void testHelpCommand(CommandSource source) {
      // no response
    }
  }

  @CommandAlias("test1")
  public static final class TestCommand {

    @Suggestions("UserSuggestions")
    public List<String> suggestUsers(CommandContext<CommandSource> $, String input) {
      return Arrays.asList("alice", "bob", "clyde");
    }

    @CommandMethod("tests test <user>")
    public void testUserCommand(
      CommandSource source,
      @Argument(value = "user", suggestions = "UserSuggestions") String user
    ) {
      // no response
    }
  }*/
}
