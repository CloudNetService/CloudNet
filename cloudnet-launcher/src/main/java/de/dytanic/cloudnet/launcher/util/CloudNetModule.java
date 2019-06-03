package de.dytanic.cloudnet.launcher.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public final class CloudNetModule {

    protected final String name;

    protected final String fileName;

}