package de.dytanic.cloudnet.launcher.cnl.defaults;

import de.dytanic.cloudnet.launcher.cnl.CNLCommand;
import de.dytanic.cloudnet.launcher.cnl.CNLInterpreter;

import java.nio.file.Paths;
import java.util.Map;

public final class CNLCommandCNL extends CNLCommand {

    public CNLCommandCNL() {
        super("cnl");
    }

    @Override
    public void execute(Map<String, String> variables, String commandLine, String... args) throws Exception {
        if (args.length > 0) {
            CNLInterpreter.runInterpreter(Paths.get(String.join(" ", args)), variables);
        }
    }
}