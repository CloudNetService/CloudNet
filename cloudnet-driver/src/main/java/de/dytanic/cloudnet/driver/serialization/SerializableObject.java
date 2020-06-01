package de.dytanic.cloudnet.driver.serialization;

import org.jetbrains.annotations.NotNull;

public interface SerializableObject {

    void write(@NotNull ProtocolBuffer buffer);

    void read(@NotNull ProtocolBuffer buffer);

}
