package de.dytanic.cloudnet.driver.service;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class ProcessSnapshot {

    private long heapUsageMemory, noHeapUsageMemory, maxHeapMemory;

    private int currentLoadedClassCount;

    private long totalLoadedClassCount, unloadedClassCount;

    private Collection<ThreadSnapshot> threads;

    private double cpuUsage;

}