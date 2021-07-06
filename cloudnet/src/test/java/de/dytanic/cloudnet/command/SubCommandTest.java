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

package de.dytanic.cloudnet.command;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.anyStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.integer;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import de.dytanic.cloudnet.command.sub.SubCommand;
import de.dytanic.cloudnet.command.sub.SubCommandArgumentWrapper;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.Properties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class SubCommandTest {

  @Test
  public void testSubCommandWithExactArgsLength() {
    AtomicReference<String> commandResponse = new AtomicReference<>();

    SubCommand subCommand = new SubCommand(
      anyStringIgnoreCase("arg1", "arg1-alias1"),
      exactStringIgnoreCase("arg2"),
      dynamicString("arg3-dyn"),
      integer("arg4-int", "invalid input")
    ) {
      @Override
      public void execute(SubCommand subCommand, ICommandSender sender, String command, SubCommandArgumentWrapper args,
        String commandLine, Properties properties, Map<String, Object> internalProperties) {
        commandResponse
          .set(args.argument(0) + " " + args.argument(1) + " " + args.argument(2) + " " + args.argument(3));
        assertTrue(args.argument(3) instanceof Integer);
        assertEquals(args.argument("arg4-int").get(), args.argument(3));
      }
    };

    assertEquals(-1, subCommand.getMinArgs());
    assertEquals(-1, subCommand.getMaxArgs());
    assertEquals("arg1 arg2 <arg3-dyn> <arg4-int>", subCommand.getArgsAsString());

    SubCommandHandler handler = new SubCommandHandler(Collections.singletonList(subCommand), "test");

    ICommandMap commandMap = new DefaultCommandMap();
    commandMap.registerCommand(handler);

    subCommand.setMinArgs(3).setMaxArgs(4);
    assertEquals("arg1 arg2 <arg3-dyn> [arg4-int]", subCommand.getArgsAsString());
    subCommand.setMinArgs(-1).setMaxArgs(-1);

    commandMap.dispatchCommand(new DriverCommandSender(new ArrayList<>()), "test arg1 arg2 testval 123");

    assertEquals("arg1 arg2 testval 123", commandResponse.get());

    subCommand.setMinArgs(4).setMaxArgs(Integer.MAX_VALUE);
    assertEquals("arg1 arg2 <arg3-dyn> <arg4-int> ...", subCommand.getArgsAsString());

    Collection<String> responses = new ArrayList<>();
    commandMap.dispatchCommand(new DriverCommandSender(responses), "test arg1 arg2 testval 123 4566");
    assertArrayEquals(new String[]{"invalid input"}, responses.toArray(new String[0]));


  }

}
