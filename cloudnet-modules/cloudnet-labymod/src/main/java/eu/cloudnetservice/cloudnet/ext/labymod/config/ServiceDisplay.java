package eu.cloudnetservice.cloudnet.ext.labymod.config;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

import java.util.function.Function;

public class ServiceDisplay {

    private boolean enabled;
    private DisplayType displayType;
    private String format;

    public ServiceDisplay(boolean enabled, DisplayType displayType, String format) {
        this.enabled = enabled;
        this.displayType = displayType;
        this.format = format;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DisplayType getDisplayType() {
        return this.displayType;
    }

    public void setDisplayType(DisplayType displayType) {
        this.displayType = displayType;
    }

    public String getDisplay(ServiceInfoSnapshot serviceInfoSnapshot) {
        return this.format.replace("%display%", this.displayType.getDisplay(serviceInfoSnapshot));
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public enum DisplayType {
        TASK(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getTaskName()),
        SERVICE(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getName()),
        FIRST_GROUP(serviceInfoSnapshot -> serviceInfoSnapshot.getConfiguration().getGroups().length == 0 ? null : serviceInfoSnapshot.getConfiguration().getGroups()[0]),
        LAST_GROUP(serviceInfoSnapshot -> serviceInfoSnapshot.getConfiguration().getGroups().length == 0 ? null : serviceInfoSnapshot.getConfiguration().getGroups()[serviceInfoSnapshot.getConfiguration().getGroups().length - 1]);

        private Function<ServiceInfoSnapshot, String> function;

        DisplayType(Function<ServiceInfoSnapshot, String> function) {
            this.function = function;
        }

        public String getDisplay(ServiceInfoSnapshot serviceInfoSnapshot) {
            return serviceInfoSnapshot != null ? this.function.apply(serviceInfoSnapshot) : null;
        }
    }

}
