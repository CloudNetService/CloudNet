package de.dytanic.cloudnet.driver;

import java.util.Optional;

public class CloudNetDriverSafe {

    public static Optional<CloudNetDriver> getDriver() {
        return Optional.ofNullable(CloudNetDriver.getInstance());
    }

}
