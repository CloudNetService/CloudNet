package de.dytanic.cloudnet.launcher.cnl.defaults;

import de.dytanic.cloudnet.launcher.cnl.CNLCommand;

import java.util.Arrays;
import java.util.Map;

public final class CNLCommandVar extends CNLCommand {

    public CNLCommandVar() {
        super("var");
    }


    @Override
    public void execute(Map<String, String> variables, String commandLine, String... args) {
        if (args.length > 1) {
            variables.put(args[0], String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        }
    }
}