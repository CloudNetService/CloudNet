package de.dytanic.cloudnet.driver.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;

@Data
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public final class ProcessConfiguration {

    protected ServiceEnvironmentType environment;

    protected int maxHeapMemorySize;

    protected Collection<String> jvmOptions;

}