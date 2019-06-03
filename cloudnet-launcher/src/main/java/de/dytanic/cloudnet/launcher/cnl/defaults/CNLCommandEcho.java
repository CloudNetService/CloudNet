package de.dytanic.cloudnet.launcher.cnl.defaults;

import de.dytanic.cloudnet.launcher.cnl.CNLCommand;

import java.util.Map;

public final class CNLCommandEcho extends CNLCommand {

    public CNLCommandEcho() {
        super("echo");
    }

    @Override
    public void execute(Map<String, String> variables, String commandLine, String... args) {

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < args.length; stringBuilder.append(args[i++]).append(" "))
            ;

        System.out.println(stringBuilder.substring(0, stringBuilder.length() == 0 ? stringBuilder.length() : stringBuilder.length() - 1));
    }
}