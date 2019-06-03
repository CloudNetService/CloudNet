package de.dytanic.cloudnet.driver.event;

public interface ICancelable {

    void setCancelled(boolean value);

    boolean isCancelled();

}