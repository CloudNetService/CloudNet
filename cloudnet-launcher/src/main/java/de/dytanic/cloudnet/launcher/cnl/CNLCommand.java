package de.dytanic.cloudnet.launcher.cnl;

import lombok.Getter;

import java.util.Map;

@Getter
public abstract class CNLCommand {

    protected String name;

    public CNLCommand(String name) {
        this.name = name;
    }

    public abstract void execute(Map<String, String> variables, String commandLine, String... args) throws Exception;
}