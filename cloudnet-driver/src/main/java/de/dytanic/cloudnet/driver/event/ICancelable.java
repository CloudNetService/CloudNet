package de.dytanic.cloudnet.driver.event;

@Deprecated
public interface ICancelable {

    boolean isCancelled();

    void setCancelled(boolean value);

}