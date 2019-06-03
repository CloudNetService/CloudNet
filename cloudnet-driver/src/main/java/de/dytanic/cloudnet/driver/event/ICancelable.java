package de.dytanic.cloudnet.driver.event;

public interface ICancelable {

    boolean isCancelled();

    void setCancelled(boolean value);

}