package de.dytanic.cloudnet.ext.bridge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyFallback implements Comparable<ProxyFallback> {

    protected String task, permission;
    private int priority;

    @Override
    public int compareTo(ProxyFallback o) {
        return priority + o.priority;
    }
}