package de.dytanic.cloudnet.launcher.cnl.install;

import de.dytanic.cloudnet.launcher.cnl.CNLCommand;

import java.util.Map;

public class CNLCommandRepo extends CNLCommand {

    private final Map<String, String> repositories;

    public CNLCommandRepo(Map<String, String> repositories) {
        super("repo");
        this.repositories = repositories;
    }

    @Override
    public void execute(Map<String, String> variables, String commandLine, String... args) {
        if (args.length == 2) {
            this.repositories.put(args[0], args[1]);
        }
    }
}
