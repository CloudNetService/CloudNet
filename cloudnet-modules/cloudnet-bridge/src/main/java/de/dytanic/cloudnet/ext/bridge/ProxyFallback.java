package de.dytanic.cloudnet.ext.bridge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyFallback implements Comparable<ProxyFallback> {

    private int priority;

    protected String task, permission;

    @Override
    public int compareTo(ProxyFallback o)
    {
        return priority + o.priority;
    }
}