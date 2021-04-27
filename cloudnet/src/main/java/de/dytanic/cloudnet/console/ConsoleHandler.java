package de.dytanic.cloudnet.console;

import org.jetbrains.annotations.NotNull;

public class ConsoleHandler<H> {

    private boolean enabled;
    private final H handler;

    public ConsoleHandler(boolean enabled, @NotNull H handler) {
        this.enabled = enabled;
        this.handler = handler;
    }

    public ConsoleHandler(@NotNull H handler) {
        this(true, handler);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public @NotNull H getHandler() {
        return this.handler;
    }
}
