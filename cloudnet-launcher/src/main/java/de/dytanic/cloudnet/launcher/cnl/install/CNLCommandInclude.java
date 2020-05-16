package de.dytanic.cloudnet.launcher.cnl.install;

import de.dytanic.cloudnet.launcher.cnl.CNLCommand;
import de.dytanic.cloudnet.launcher.version.util.Dependency;

import java.util.Collection;
import java.util.Map;

public final class CNLCommandInclude extends CNLCommand {

    private final Collection<Dependency> includes;

    public CNLCommandInclude(Collection<Dependency> includes) {
        super("include");
        this.includes = includes;
    }

    @Override
    public void execute(Map<String, String> variables, String commandLine, String... args) {
        if (args.length >= 4) {
            Dependency dependency = new Dependency(args[0], args[1], args[2], args[3], args.length == 5 ? args[4] : null);

            if (!this.includes.contains(dependency)) {
                this.includes.add(dependency);
            }
        }
    }
}