package de.dytanic.cloudnet.command;

import de.dytanic.cloudnet.common.Properties;
import org.junit.Assert;
import org.junit.Test;

public final class DefaultCommandMapTest {

    private String name;

    private String b;

    @Test
    public void testMap() {
        ICommandMap commandMap = new DefaultCommandMap();

        ICommandSender commandSender = new ICommandSender() {

            @Override
            public void sendMessage(String message) {
                b = message;
            }

            @Override
            public void sendMessage(String... messages) {

            }

            @Override
            public boolean hasPermission(String permission) {
                return true;
            }

            @Override
            public String getName() {
                return "test";
            }
        };

        final Command command = new Command() {

            {
                names = new String[]{"test"};
                description = "description";
                prefix = "test";
            }

            @Override
            public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
                Assert.assertEquals(2, args.length);
                Assert.assertEquals("val", args[1]);

                DefaultCommandMapTest.this.name = args[0];

                sender.sendMessage("Hello, world!");
            }
        };

        commandMap.registerCommand(command);

        Assert.assertNotNull(commandMap.getCommand("TEST"));
        Assert.assertNotNull(commandMap.getCommand("test:test"));
        Assert.assertEquals(2, commandMap.getCommandNames().size());
        Assert.assertTrue(commandMap.dispatchCommand(commandSender, "test:test Dytanic val"));

        commandMap.unregisterCommand(command.getClass());
        Assert.assertEquals(0, commandMap.getCommandNames().size());

        Assert.assertNotNull(this.name);
        Assert.assertEquals("Dytanic", this.name);
        Assert.assertEquals("Hello, world!", b);
    }

}