package de.dytanic.cloudnet.driver.service;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public final class ProcessConfiguration {

  protected ServiceEnvironmentType environment;

  protected int maxHeapMemorySize;

  protected Collection<String> jvmOptions;

}