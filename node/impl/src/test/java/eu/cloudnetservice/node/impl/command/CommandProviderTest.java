/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.command;

import dev.derklaro.aerogel.binding.BindingBuilder;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.event.DefaultEventManager;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.impl.command.defaults.DefaultCommandProvider;
import eu.cloudnetservice.node.impl.command.source.DriverCommandSource;
import eu.cloudnetservice.node.impl.junit.EnableServicesInject;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.NoSuchCommandException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Iterables;

@EnableServicesInject
public final class CommandProviderTest {

  private static CommandProvider commandProvider;

  @BeforeAll
  public static void initCommandProvider() {
    // install the required bindings to construct the command provider
    var layer = InjectionLayer.boot();
    layer.install(BindingBuilder.create().bind(EventManager.class).toConstructing(DefaultEventManager.class));
    layer.install(BindingBuilder.create().bind(CommandProvider.class).toConstructing(DefaultCommandProvider.class));

    // get the command provider instance
    commandProvider = layer.instance(CommandProvider.class);

    // register two test commands
    commandProvider.register(new TestCommand());
    commandProvider.register(new HelpTestCommand());
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
    Assertions.assertEquals(testCommand, testCommandByAlias);
  }

  @Test
  public void testStaticCommandSuggestions() {
    var source = new DriverCommandSource();

    var rootSuggestions = commandProvider.suggest(source, "tests");
    Assertions.assertEquals(1, rootSuggestions.size());
    Assertions.assertEquals("tests", rootSuggestions.getFirst());

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
      TaskUtil.getOrDefault(commandProvider.execute(source, "non existing command"), null);
    } catch (CompletionException exception) {
      Assertions.assertEquals(NoSuchCommandException.class, exception.getCause().getClass());
    }
  }

  @Test
  public void testCommandUnregister() {
    // the confirmation command is always registered so there are 3 commands
    Assertions.assertEquals(3, commandProvider.commands().size());
    commandProvider.unregister(this.getClass().getClassLoader());
    Assertions.assertEquals(0, commandProvider.commands().size());
  }

  public static final class HelpTestCommand {

    @Command("help")
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

    @Command("tests test <user>")
    public void testUserCommand(
      CommandSource source,
      @Argument(value = "user", suggestions = "UserSuggestions") String user
    ) {
      // no response
    }
  }
}
