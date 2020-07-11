package de.dytanic.cloudnet.console;

public class ConsoleHandler<H> {
    private boolean enabled;
    private final H handler;

    public ConsoleHandler(boolean enabled, H handler) {
        this.enabled = enabled;
        this.handler = handler;
    }

    public ConsoleHandler(H handler) {
        this(true, handler);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public H getHandler() {
        return this.handler;
    }
}
