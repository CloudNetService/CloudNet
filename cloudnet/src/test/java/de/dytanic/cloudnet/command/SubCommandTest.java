package de.dytanic.cloudnet.command;

import de.dytanic.cloudnet.command.sub.SubCommand;
import de.dytanic.cloudnet.command.sub.SubCommandArgumentWrapper;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.Properties;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.*;
import static org.junit.Assert.*;

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
            public void execute(SubCommand subCommand, ICommandSender sender, String command, SubCommandArgumentWrapper args, String commandLine, Properties properties, Map<String, Object> internalProperties) {
                commandResponse.set(args.argument(0) + " " + args.argument(1) + " " + args.argument(2) + " " + args.argument(3));
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
