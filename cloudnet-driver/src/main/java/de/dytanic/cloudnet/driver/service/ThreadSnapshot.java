package de.dytanic.cloudnet.driver.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ThreadSnapshot {

    private long id;

    private String name;

    private Thread.State threadState;

    private boolean daemon;

    private int priority;

}