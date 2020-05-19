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
                DefaultCommandMapTest.this.b = message;
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
                this.names = new String[]{"test asdf"};
                this.description = "description";
                this.prefix = "test 123";
            }

            @Override
            public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
                Assert.assertEquals(3, args.length);
                Assert.assertEquals("Dytanic", args[0]);
                Assert.assertEquals("derrop", args[1]);
                Assert.assertEquals("val 1 2 3 4", args[2]);

                DefaultCommandMapTest.this.name = args[0];

                sender.sendMessage("Hello, world!");
            }
        };

        commandMap.registerCommand(command);

        Assert.assertNotNull(commandMap.getCommand("TEST asdf"));
        Assert.assertNotNull(commandMap.getCommand("test 123:test asdf"));
        Assert.assertEquals(2, commandMap.getCommandNames().size());
        Assert.assertTrue(commandMap.dispatchCommand(commandSender, "\"test 123:test asdf\" \"Dytanic\" derrop \"val 1 2 3 4\""));

        commandMap.unregisterCommand(command.getClass());
        Assert.assertEquals(0, commandMap.getCommandNames().size());

        Assert.assertNotNull(this.name);
        Assert.assertEquals("Dytanic", this.name);
        Assert.assertEquals("Hello, world!", this.b);
    }

}