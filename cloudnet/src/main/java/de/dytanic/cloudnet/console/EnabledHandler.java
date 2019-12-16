package de.dytanic.cloudnet.console;

public class EnabledHandler<H> {

    private boolean enabled;
    private H handler;

    public EnabledHandler(boolean enabled, H handler) {
        this.enabled = enabled;
        this.handler = handler;
    }

    public EnabledHandler(H handler) {
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
