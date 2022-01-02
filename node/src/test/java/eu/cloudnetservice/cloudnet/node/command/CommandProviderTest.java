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

package eu.cloudnetservice.cloudnet.node.command;


import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.NoSuchCommandException;
import eu.cloudnetservice.cloudnet.driver.NodeTestUtility;
import eu.cloudnetservice.cloudnet.driver.event.DefaultEventManager;
import eu.cloudnetservice.cloudnet.node.command.annotation.CommandAlias;
import eu.cloudnetservice.cloudnet.node.command.defaults.DefaultCommandProvider;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.cloudnet.node.command.source.DriverCommandSource;
import eu.cloudnetservice.cloudnet.node.console.Console;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Iterables;

public final class CommandProviderTest {

  private static final CommandProvider commandProvider = new DefaultCommandProvider(Mockito.mock(Console.class));

  @BeforeAll
  public static void initNode() {
    var node = NodeTestUtility.mockAndSetDriverInstance();
    Mockito.when(node.commandProvider()).thenReturn(commandProvider);
    Mockito.when(node.eventManager()).thenReturn(new DefaultEventManager());
    commandProvider.register(new CommandTest());
    commandProvider.register(new CommandHelpTest());
  }

  @Test
  public void testCommandRegistration() {
    var testCommand = commandProvider.command("tests");
    Assertions.assertNotNull(testCommand);
    Assertions.assertEquals(1, testCommand.usage().size());
    Assertions.assertEquals("tests test <user>", Iterables.firstOf(testCommand.usage()));

    var testCommandByAlias = commandProvider.command("test1");
    Assertions.assertNotNull(testCommandByAlias);
    Assertions.assertNotEquals("test1", testCommand.name());
    Assertions.assertEquals(testCommandByAlias, testCommandByAlias);
  }

  @Test
  public void testStaticCommandSuggestions() {
    var source = new DriverCommandSource();

    var rootSuggestions = commandProvider.suggest(source, "tests");
    Assertions.assertEquals(2, rootSuggestions.size());
    Assertions.assertEquals(Arrays.asList("help", "tests"), rootSuggestions);

    var subSuggestions = commandProvider.suggest(source, "tests ");
    Assertions.assertEquals(1, subSuggestions.size());
    Assertions.assertEquals("test", Iterables.firstOf(subSuggestions));
  }

  @Test
  public void testDynamicCommandSuggestions() {
    var source = new DriverCommandSource();

    var suggestions = commandProvider.suggest(source, "tests test ");
    Assertions.assertEquals(3, suggestions.size());
    Assertions.assertEquals(Arrays.asList("alice", "bob", "clyde"), suggestions);
  }

  @Test
  public void testCommandNotFound() {
    var source = new DriverCommandSource();

    try {
      commandProvider.execute(source, "non existing command").fireExceptionOnFailure().getOrNull();
    } catch (CompletionException exception) {
      Assertions.assertEquals(NoSuchCommandException.class, exception.getCause().getClass());
    }
  }

  @Test
  public void testCommandUnregister() {
    Assertions.assertEquals(2, commandProvider.commands().size());
    commandProvider.unregister(this.getClass().getClassLoader());
    Assertions.assertEquals(0, commandProvider.commands().size());
  }

  public static final class CommandHelpTest {

    @CommandMethod("help")
    public void testHelpCommand(CommandSource source) {
      // no response
    }
  }

  @CommandAlias("test1")
  public static final class CommandTest {

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
  }
}
