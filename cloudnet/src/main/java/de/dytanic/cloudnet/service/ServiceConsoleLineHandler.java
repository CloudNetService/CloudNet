package de.dytanic.cloudnet.service;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ServiceConsoleLineHandler {

    void handleLine(@NotNull IServiceConsoleLogCache source, @NotNull String line);
}
